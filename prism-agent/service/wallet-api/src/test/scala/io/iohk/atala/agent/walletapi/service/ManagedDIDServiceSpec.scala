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

object ManagedDIDServiceSpec extends ZIOSpecDefault {

  private trait TestDIDService extends DIDService {
    def getPublishedOperations: UIO[Seq[SignedPrismDIDOperation]]
  }

  private def testDIDServiceLayer = ZLayer.fromZIO {
    Ref.make(Seq.empty[SignedPrismDIDOperation]).map { store =>
      new TestDIDService {
        override def scheduleOperation(
            signOperation: SignedPrismDIDOperation
        ): IO[error.DIDOperationError, ScheduleDIDOperationOutcome] = {
          store
            .update(_.appended(signOperation))
            .as(ScheduleDIDOperationOutcome(signOperation.operation.did, signOperation.operation, ArraySeq.empty))
        }

        override def resolveDID(did: PrismDID): IO[error.DIDResolutionError, Option[(DIDMetadata, DIDData)]] = ZIO.none

        override def getScheduledDIDOperationDetail(
            operationId: Array[Byte]
        ): IO[error.DIDOperationError, Option[ScheduledDIDOperationDetail]] =
          ZIO.some(ScheduledDIDOperationDetail(ScheduledDIDOperationStatus.Pending))

        override def getPublishedOperations: UIO[Seq[SignedPrismDIDOperation]] = store.get
      }
    }
  }

  private def managedDIDServiceLayer: ULayer[TestDIDService & ManagedDIDService] =
    (DIDOperationValidator.layer() ++ testDIDServiceLayer) >+> ManagedDIDService.inMemoryStorage

  private def generateDIDTemplate(
      publicKeys: Seq[DIDPublicKeyTemplate] = Nil,
      services: Seq[Service] = Nil
  ): ManagedDIDTemplate = ManagedDIDTemplate(publicKeys, services)

  override def spec =
    suite("ManagedDIDService")(publishStoredDIDSpec, createAndStoreDIDSpec).provideLayer(managedDIDServiceLayer)

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
      } yield assert(keyPairs.keys)(hasSameElements(Seq("key1", "key2", ManagedDIDService.DEFAULT_MASTER_KEY_ID)))
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
        createOperation <- svc.nonSecretStorage.getManagedDIDState(did)
        publicKeys <- ZIO.fromOption(createOperation.collect { case ManagedDIDState.Created(operation) =>
          operation.publicKeys
        })
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

}
