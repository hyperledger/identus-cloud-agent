package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.error.{CreateManagedDIDError, PublishManagedDIDError}
import io.iohk.atala.agent.walletapi.model.{DIDPublicKeyTemplate, ManagedDIDState, ManagedDIDTemplate}
import io.iohk.atala.castor.core.model.did.{
  DIDData,
  DIDMetadata,
  PrismDID,
  PrismDIDOperation,
  ScheduleDIDOperationOutcome,
  ScheduledDIDOperationDetail,
  ScheduledDIDOperationStatus,
  Service,
  SignedPrismDIDOperation,
  VerificationRelationship
}
import io.iohk.atala.castor.core.model.error
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*
import zio.test.*
import zio.test.Assertion.*

import scala.collection.immutable.ArraySeq
import io.iohk.atala.test.container.PostgresTestContainerSupport
import io.iohk.atala.agent.walletapi.sql.JdbcDIDSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcDIDNonSecretStorage
import io.iohk.atala.test.container.DBTestUtils
import io.iohk.atala.castor.core.model.did.InternalKeyPurpose
import io.iohk.atala.agent.walletapi.model.error.UpdateManagedDIDError
import io.iohk.atala.agent.walletapi.model.UpdateManagedDIDAction

object ManagedDIDServiceSpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  private trait TestDIDService extends DIDService {
    def getPublishedOperations: UIO[Seq[SignedPrismDIDOperation]]
    def setOperationStatus(status: ScheduledDIDOperationStatus): UIO[Unit]
    def setResolutionResult(result: Option[(DIDMetadata, DIDData)]): UIO[Unit]
  }

  private def testDIDServiceLayer = ZLayer.fromZIO {
    for {
      operationStore <- Ref.make(Seq.empty[SignedPrismDIDOperation])
      statusStore <- Ref.make[ScheduledDIDOperationStatus](ScheduledDIDOperationStatus.Pending)
      resolutionStore <- Ref.make[Option[(DIDMetadata, DIDData)]](None)
    } yield new TestDIDService {
      override def scheduleOperation(
          signOperation: SignedPrismDIDOperation
      ): IO[error.DIDOperationError, ScheduleDIDOperationOutcome] = {
        operationStore
          .update(_.appended(signOperation))
          .as(ScheduleDIDOperationOutcome(signOperation.operation.did, signOperation.operation, ArraySeq.empty))
      }

      override def resolveDID(did: PrismDID): IO[error.DIDResolutionError, Option[(DIDMetadata, DIDData)]] =
        resolutionStore.get

      override def getScheduledDIDOperationDetail(
          operationId: Array[Byte]
      ): IO[error.DIDOperationError, Option[ScheduledDIDOperationDetail]] =
        statusStore.get.map(ScheduledDIDOperationDetail(_)).asSome

      override def getPublishedOperations: UIO[Seq[SignedPrismDIDOperation]] = operationStore.get

      override def setOperationStatus(status: ScheduledDIDOperationStatus): UIO[Unit] = statusStore.set(status)

      override def setResolutionResult(result: Option[(DIDMetadata, DIDData)]): UIO[Unit] = resolutionStore.set(result)
    }
  }

  private def jdbcStorageLayer =
    pgContainerLayer >+> transactorLayer >+> (JdbcDIDSecretStorage.layer ++ JdbcDIDNonSecretStorage.layer)

  private def managedDIDServiceLayer =
    (DIDOperationValidator.layer() ++ testDIDServiceLayer) >+> ManagedDIDService.layer

  private def generateDIDTemplate(
      publicKeys: Seq[DIDPublicKeyTemplate] = Nil,
      services: Seq[Service] = Nil
  ): ManagedDIDTemplate = ManagedDIDTemplate(publicKeys, services)

  private def resolutionResult(deactivated: Boolean = false): (DIDMetadata, DIDData) = {
    val metadata = DIDMetadata(
      lastOperationHash = ArraySeq.empty,
      deactivated = deactivated
    )
    val didData = DIDData(
      id = PrismDID.buildCanonicalFromSuffix("0" * 64).toOption.get,
      publicKeys = Nil,
      internalKeys = Nil,
      services = Nil
    )
    metadata -> didData
  }

  private val initPublishedDID =
    for {
      svc <- ZIO.service[ManagedDIDService]
      testDIDSvc <- ZIO.service[TestDIDService]
      did <- svc.createAndStoreDID(generateDIDTemplate()).map(_.asCanonical)
      _ <- svc.publishStoredDID(did)
      _ <- testDIDSvc.setOperationStatus(ScheduledDIDOperationStatus.Confirmed)
      _ <- svc.syncManagedDIDState
    } yield did

  override def spec = {
    val testSuite =
      suite("ManagedDIDService")(
        publishStoredDIDSpec,
        createAndStoreDIDSpec,
        updateManagedDIDSpec
      ) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

    testSuite.provideLayer(jdbcStorageLayer >+> managedDIDServiceLayer)
  } @@ TestAspect.tag("dev") // TODO: remove

  private val publishStoredDIDSpec =
    suite("publishStoredDID")(
      test("publish stored DID if exists") {
        val template = generateDIDTemplate()
        for {
          svc <- ZIO.service[ManagedDIDService]
          testDIDSvc <- ZIO.service[TestDIDService]
          did <- svc.createAndStoreDID(template).map(_.asCanonical)
          createOp <- svc.nonSecretStorage.getManagedDIDState(did).collect(()) {
            case Some(ManagedDIDState.Created(op)) => op
          }
          opsBefore <- testDIDSvc.getPublishedOperations
          _ <- svc.publishStoredDID(did)
          opsAfter <- testDIDSvc.getPublishedOperations
        } yield assert(opsBefore)(isEmpty) &&
          assert(opsAfter.map(_.operation))(hasSameElements(Seq(createOp)))
      },
      test("fail when publish non-existing DID") {
        val did = PrismDID.buildLongFormFromOperation(PrismDIDOperation.Create(Nil, Nil, Nil)).asCanonical
        val result = ZIO.serviceWithZIO[ManagedDIDService](_.publishStoredDID(did))
        assertZIO(result.exit)(fails(isSubtype[PublishManagedDIDError.DIDNotFound](anything)))
      },
      test("set status to publication pending after publishing") {
        val template = generateDIDTemplate()
        for {
          svc <- ZIO.service[ManagedDIDService]
          did <- svc.createAndStoreDID(template).map(_.asCanonical)
          stateBefore <- svc.nonSecretStorage.getManagedDIDState(did)
          _ <- svc.publishStoredDID(did)
          stateAfter <- svc.nonSecretStorage.getManagedDIDState(did)
        } yield assert(stateBefore)(isSome(isSubtype[ManagedDIDState.Created](anything)))
          && assert(stateAfter)(isSome(isSubtype[ManagedDIDState.PublicationPending](anything)))
      },
      test("do not re-publish when publishing already published DID") {
        val template = generateDIDTemplate()
        for {
          svc <- ZIO.service[ManagedDIDService]
          testDIDSvc <- ZIO.service[TestDIDService]
          did <- initPublishedDID // 1st publish
          _ <- svc.publishStoredDID(did) // 2nd publish
          opsAfter <- testDIDSvc.getPublishedOperations
        } yield assert(opsAfter)(hasSize(equalTo(1)))
      }
    )

  private val createAndStoreDIDSpec = suite("createAndStoreDID")(
    test("create and store DID list in DIDNonSecretStorage") {
      val template = generateDIDTemplate()
      for {
        svc <- ZIO.service[ManagedDIDService]
        didsBefore <- svc.nonSecretStorage.listManagedDID
        did <- svc.createAndStoreDID(template).map(_.asCanonical)
        didsAfter <- svc.nonSecretStorage.listManagedDID
      } yield assert(didsBefore)(isEmpty) &&
        assert(didsAfter.keySet)(hasSameElements(Seq(did)))
    },
    test("create and store DID secret in DIDSecretStorage") {
      val template = generateDIDTemplate(
        publicKeys = Seq(
          DIDPublicKeyTemplate("key1", VerificationRelationship.Authentication),
          DIDPublicKeyTemplate("key2", VerificationRelationship.KeyAgreement)
        )
      )
      for {
        svc <- ZIO.service[ManagedDIDService]
        did <- svc.createAndStoreDID(template).map(_.asCanonical)
        keyPairs <- svc.secretStorage.listKeys(did)
      } yield assert(keyPairs.map(_._1))(hasSameElements(Seq("key1", "key2", ManagedDIDService.DEFAULT_MASTER_KEY_ID)))
    },
    test("created DID have corresponding public keys in CreateOperation") {
      val template = generateDIDTemplate(
        publicKeys = Seq(
          DIDPublicKeyTemplate("key1", VerificationRelationship.Authentication),
          DIDPublicKeyTemplate("key2", VerificationRelationship.KeyAgreement),
          DIDPublicKeyTemplate("key3", VerificationRelationship.AssertionMethod)
        )
      )
      for {
        svc <- ZIO.service[ManagedDIDService]
        did <- svc.createAndStoreDID(template).map(_.asCanonical)
        state <- svc.nonSecretStorage.getManagedDIDState(did)
        createOperation <- ZIO.fromOption(state.collect { case ManagedDIDState.Created(operation) => operation })
        publicKeys = createOperation.publicKeys
      } yield assert(publicKeys.map(i => i.id -> i.purpose))(
        hasSameElements(
          Seq(
            "key1" -> VerificationRelationship.Authentication,
            "key2" -> VerificationRelationship.KeyAgreement,
            "key3" -> VerificationRelationship.AssertionMethod
          )
        )
      )
    },
    test("created DID contain at least 1 master key in CreateOperation") {
      for {
        svc <- ZIO.service[ManagedDIDService]
        did <- svc.createAndStoreDID(generateDIDTemplate()).map(_.asCanonical)
        state <- svc.nonSecretStorage.getManagedDIDState(did)
        createOperation <- ZIO.fromOption(state.collect { case ManagedDIDState.Created(operation) => operation })
        internalKeys = createOperation.internalKeys
      } yield assert(internalKeys.map(_.purpose))(contains(InternalKeyPurpose.Master))
    },
    test("validate DID before persisting it in storage") {
      // this template will fail during validation for reserved key id
      val template = generateDIDTemplate(
        publicKeys = Seq(
          DIDPublicKeyTemplate("master0", VerificationRelationship.Authentication)
        )
      )
      val result = ZIO.serviceWithZIO[ManagedDIDService](_.createAndStoreDID(template))
      assertZIO(result.exit)(fails(isSubtype[CreateManagedDIDError.InvalidArgument](anything)))
    }
  )

  private val updateManagedDIDSpec =
    suite("updateManagedDID")(
      test("update stored and published DID") {
        val template = generateDIDTemplate()
        for {
          svc <- ZIO.service[ManagedDIDService]
          testDIDSvc <- ZIO.service[TestDIDService]
          did <- initPublishedDID
          _ <- testDIDSvc.setResolutionResult(Some(resolutionResult()))
          _ <- svc.updateManagedDID(did, Seq(UpdateManagedDIDAction.RemoveKey("key-1")))
          operations <- testDIDSvc.getPublishedOperations
        } yield assert(operations.map(_.operation))(exists(isSubtype[PrismDIDOperation.Update](anything)))
      },
      test("fail on updating non-existing DID") {
        val did = PrismDID.buildCanonicalFromSuffix("0" * 64).toOption.get
        val effect = for {
          svc <- ZIO.service[ManagedDIDService]
          _ <- svc.updateManagedDID(did, Seq(UpdateManagedDIDAction.RemoveKey("key-1")))
        } yield ()
        assertZIO(effect.exit)(fails(isSubtype[UpdateManagedDIDError.DIDNotFound](anything)))
      },
      test("fail on updating unpublished DID") {
        val template = generateDIDTemplate()
        val effect = for {
          svc <- ZIO.service[ManagedDIDService]
          did <- svc.createAndStoreDID(template).map(_.asCanonical)
          _ <- svc.updateManagedDID(did, Seq(UpdateManagedDIDAction.RemoveKey("key-1")))
        } yield ()
        assertZIO(effect.exit)(fails(isSubtype[UpdateManagedDIDError.DIDNotPublished](anything)))
      },
      test("fail on deactivated DID") {
        val template = generateDIDTemplate()
        val effect = for {
          svc <- ZIO.service[ManagedDIDService]
          testDIDSvc <- ZIO.service[TestDIDService]
          did <- initPublishedDID
          // set did as deactivated
          _ <- testDIDSvc.setResolutionResult(Some(resolutionResult(deactivated = true)))
          _ <- svc.updateManagedDID(did, Seq(UpdateManagedDIDAction.RemoveKey("key-1")))
        } yield ()
        assertZIO(effect.exit)(fails(isSubtype[UpdateManagedDIDError.DIDAlreadyDeactivated](anything)))
      },
      test("validate constructed operation before submitting an operation") {
        val template = generateDIDTemplate()
        for {
          svc <- ZIO.service[ManagedDIDService]
          testDIDSvc <- ZIO.service[TestDIDService]
          did <- initPublishedDID
          _ <- testDIDSvc.setResolutionResult(Some(resolutionResult()))
          // catch expected validation error and assert that operation was not submitted
          _ <- svc.updateManagedDID(did, Nil).catchSome { case _: UpdateManagedDIDError.InvalidOperation => ZIO.unit }
          operations <- testDIDSvc.getPublishedOperations
        } yield assert(operations)(hasSize(equalTo(1))) // only 1 publish and no update operation
      }
    )

}
