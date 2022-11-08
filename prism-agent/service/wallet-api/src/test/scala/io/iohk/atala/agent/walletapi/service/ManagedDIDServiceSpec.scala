package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.KeyGeneratorWrapper
import io.iohk.atala.agent.walletapi.model.error.{CreateManagedDIDError, PublishManagedDIDError, UpdateManagedDIDError}
import io.iohk.atala.agent.walletapi.model.{
  CommitmentPurpose,
  DIDPublicKeyTemplate,
  ManagedDIDCreateTemplate,
  ManagedDIDUpdatePatch,
  ManagedDIDUpdateTemplate,
  StagingDIDUpdateSecret
}
import io.iohk.atala.agent.walletapi.model.ECCoordinates.*
import io.iohk.atala.castor.core.model.did.{
  DIDDocument,
  DIDStatePatch,
  DIDStorage,
  EllipticCurve,
  LongFormPrismDIDV1,
  PrismDIDV1,
  PublicKey,
  PublicKeyJwk,
  PublishedDIDOperation,
  PublishedDIDOperationOutcome,
  Service,
  ServiceType,
  UpdateOperationDelta,
  VerificationRelationship
}
import io.iohk.atala.castor.core.model.error
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.net.URI

object ManagedDIDServiceSpec extends ZIOSpecDefault {

  private trait TestDIDService extends DIDService {
    def getPublishedCreateOperations: UIO[Seq[PublishedDIDOperation.Create]]
    def getPublishedUpdateOperations: UIO[Seq[PublishedDIDOperation.Update]]
  }

  private def testDIDServiceLayer = ZLayer.fromZIO {

    final case class InMemoryStorageRecord(
        createOps: Seq[PublishedDIDOperation.Create],
        updateOps: Seq[PublishedDIDOperation.Update]
    )

    Ref.make(InMemoryStorageRecord(Nil, Nil)).map { store =>
      new TestDIDService {
        override def createPublishedDID(
            operation: PublishedDIDOperation.Create
        ): IO[error.DIDOperationError, PublishedDIDOperationOutcome] =
          store
            .update(current => current.copy(createOps = current.createOps.appended(operation)))
            .as(
              PublishedDIDOperationOutcome(
                PrismDIDV1.fromCreateOperation(operation),
                operation,
                HexString.fromStringUnsafe("00")
              )
            )

        override def updatePublishedDID(
            operation: PublishedDIDOperation.Update
        ): IO[error.DIDOperationError, PublishedDIDOperationOutcome] =
          store
            .update(current => current.copy(updateOps = current.updateOps.appended(operation)))
            .as(
              PublishedDIDOperationOutcome(operation.did, operation, HexString.fromStringUnsafe("00"))
            )

        override def getPublishedCreateOperations: UIO[Seq[PublishedDIDOperation.Create]] = store.get.map(_.createOps)

        override def getPublishedUpdateOperations: UIO[Seq[PublishedDIDOperation.Update]] = store.get.map(_.updateOps)
      }
    }
  }

  private def managedDIDServiceLayer: ULayer[TestDIDService & ManagedDIDService] = {
    val didValidatorConfig = DIDOperationValidator.Config(50, 50)
    (DIDOperationValidator.layer(didValidatorConfig) ++ testDIDServiceLayer) >+> ManagedDIDService.inMemoryStorage
  }

  private def generateDIDCreateTemplate(
      publicKeys: Seq[DIDPublicKeyTemplate] = Nil,
      services: Seq[Service] = Nil
  ): ManagedDIDCreateTemplate = ManagedDIDCreateTemplate("testnet", publicKeys, services)

  private def generateDIDUpdateTemplate(patches: Seq[ManagedDIDUpdatePatch] = Nil): ManagedDIDUpdateTemplate =
    ManagedDIDUpdateTemplate(patches = patches)

  override def spec =
    suite("ManagedDIDService")(publishStoredDIDSpec, createAndStoreDIDSpec, updateDIDAndPublishSpec).provideLayer(
      managedDIDServiceLayer
    )

  private val publishStoredDIDSpec =
    suite("publishStoredDID")(
      test("publish stored DID if exists") {
        val template = generateDIDCreateTemplate()
        for {
          svc <- ZIO.service[ManagedDIDService]
          testDIDSvc <- ZIO.service[TestDIDService]
          did <- svc.createAndStoreDID(template).map(_.toCanonical)
          createOp <- svc.nonSecretStorage.getCreatedDID(did)
          opsBefore <- testDIDSvc.getPublishedCreateOperations
          _ <- svc.publishStoredDID(did)
          opsAfter <- testDIDSvc.getPublishedCreateOperations
        } yield assert(opsBefore)(isEmpty) &&
          assert(opsAfter)(hasSameElements(createOp.toList))
      },
      test("fail when publish non-existing DID") {
        val did = PrismDIDV1.fromCreateOperation(
          PublishedDIDOperation.Create(
            updateCommitment = HexString.fromStringUnsafe("00"),
            recoveryCommitment = HexString.fromStringUnsafe("00"),
            storage = DIDStorage.Cardano("testnet"),
            document = DIDDocument(Nil, Nil)
          )
        )
        val result = ZIO.serviceWithZIO[ManagedDIDService](_.publishStoredDID(did))
        assertZIO(result.exit)(fails(isSubtype[PublishManagedDIDError.DIDNotFound](anything)))
      },
      test("publish stored DID when provide long-form DID") {
        val template = generateDIDCreateTemplate()
        for {
          svc <- ZIO.service[ManagedDIDService]
          testDIDSvc <- ZIO.service[TestDIDService]
          longFormDID <- svc.createAndStoreDID(template)
          did = longFormDID.toCanonical
          createOp <- svc.nonSecretStorage.getCreatedDID(did)
          _ <- svc.publishStoredDID(longFormDID)
          opsAfter <- testDIDSvc.getPublishedCreateOperations
        } yield assert(opsAfter)(hasSameElements(createOp.toList))
      },
      test("flag DID as published") {
        val template = generateDIDCreateTemplate()
        for {
          svc <- ZIO.service[ManagedDIDService]
          did <- svc.createAndStoreDID(template).map(_.toCanonical)
          _ <- svc.publishStoredDID(did)
          publishedDIDs <- svc.nonSecretStorage.listPublishedDID
        } yield assert(publishedDIDs)(contains(did))
      }
    )

  private val createAndStoreDIDSpec = suite("createAndStoreDID")(
    test("create and store DID list in DIDNonSecretStorage") {
      val template = generateDIDCreateTemplate()
      for {
        svc <- ZIO.service[ManagedDIDService]
        didsBefore <- svc.nonSecretStorage.listCreatedDID
        did <- svc.createAndStoreDID(template).map(_.toCanonical)
        didsAfter <- svc.nonSecretStorage.listCreatedDID
      } yield assert(didsBefore)(isEmpty) &&
        assert(didsAfter)(hasSameElements(Seq(did)))
    },
    test("create and store DID secret in DIDSecretStorage") {
      val template = generateDIDCreateTemplate(
        publicKeys = Seq(
          DIDPublicKeyTemplate("key-1", VerificationRelationship.Authentication),
          DIDPublicKeyTemplate("key-2", VerificationRelationship.KeyAgreement)
        )
      )
      for {
        svc <- ZIO.service[ManagedDIDService]
        did <- svc.createAndStoreDID(template).map(_.toCanonical)
        updateCommitment <- svc.secretStorage.getDIDCommitmentKey(did, CommitmentPurpose.Update)
        recoveryCommitment <- svc.secretStorage.getDIDCommitmentKey(did, CommitmentPurpose.Recovery)
        keyPairs <- svc.secretStorage.listKeys(did)
      } yield assert(updateCommitment)(isSome) &&
        assert(recoveryCommitment)(isSome) &&
        assert(keyPairs.keys)(hasSameElements(Seq("key-1", "key-2")))
    },
    test("created DID have corresponding public keys in CreateOperation") {
      val template = generateDIDCreateTemplate(
        publicKeys = Seq(
          DIDPublicKeyTemplate("key-1", VerificationRelationship.Authentication),
          DIDPublicKeyTemplate("key-2", VerificationRelationship.KeyAgreement),
          DIDPublicKeyTemplate("key-3", VerificationRelationship.AssertionMethod)
        )
      )
      for {
        svc <- ZIO.service[ManagedDIDService]
        did <- svc.createAndStoreDID(template).map(_.toCanonical)
        createOperation <- svc.nonSecretStorage.getCreatedDID(did)
        publicKeys <- ZIO.fromOption(createOperation).map(_.document.publicKeys)
      } yield assert(publicKeys.map(i => i.id -> i.purposes))(
        hasSameElements(
          Seq(
            "key-1" -> Seq(VerificationRelationship.Authentication),
            "key-2" -> Seq(VerificationRelationship.KeyAgreement),
            "key-3" -> Seq(VerificationRelationship.AssertionMethod)
          )
        )
      )
    },
    test("validate DID before persisting it in storage") {
      // this template will fail during validation for duplicated id
      val template = generateDIDCreateTemplate(
        publicKeys = Seq(
          DIDPublicKeyTemplate("key-1", VerificationRelationship.Authentication),
          DIDPublicKeyTemplate("key-1", VerificationRelationship.KeyAgreement)
        )
      )
      val result = ZIO.serviceWithZIO[ManagedDIDService](_.createAndStoreDID(template))
      assertZIO(result.exit)(fails(isSubtype[CreateManagedDIDError.OperationError](anything)))
    }
  )

  private val updateDIDAndPublishSpec = suite("updateDIDAndPublish")(
    test("update DID and publish operation") {
      val createTemplate = generateDIDCreateTemplate()
      val updateTemplate = generateDIDUpdateTemplate()
      for {
        svc <- ZIO.service[ManagedDIDService]
        testDIDSvc <- ZIO.service[TestDIDService]
        did <- svc.createAndStoreDID(createTemplate).map(_.toCanonical)
        _ <- svc.publishStoredDID(did)
        _ <- svc.updateDIDAndPublish(did, updateTemplate)
        publishedUpdateOps <- testDIDSvc.getPublishedUpdateOperations
      } yield assert(publishedUpdateOps)(hasSize(equalTo(1))) && assert(publishedUpdateOps.map(_.did))(
        equalTo(Seq(did))
      )
    },
    test("generate corresponding DIDStatePatch for all ManagedDIDPatch") {
      val createTemplate = generateDIDCreateTemplate()
      val updateTemplate = generateDIDUpdateTemplate(patches =
        Seq(
          ManagedDIDUpdatePatch.AddPublicKey(
            DIDPublicKeyTemplate(
              id = "key-1",
              purpose = VerificationRelationship.Authentication
            )
          ),
          ManagedDIDUpdatePatch.AddService(
            Service(
              id = "service-1",
              `type` = ServiceType.MediatorService,
              serviceEndpoint = URI.create("https://example.com")
            )
          ),
          ManagedDIDUpdatePatch.RemovePublicKey("key-99"),
          ManagedDIDUpdatePatch.RemoveService("service-99")
        )
      )
      for {
        svc <- ZIO.service[ManagedDIDService]
        testDIDSvc <- ZIO.service[TestDIDService]
        did <- svc.createAndStoreDID(createTemplate).map(_.toCanonical)
        _ <- svc.publishStoredDID(did)
        _ <- svc.updateDIDAndPublish(did, updateTemplate)
        publishedUpdateOps <- testDIDSvc.getPublishedUpdateOperations
        patches = publishedUpdateOps.head.delta.patches
      } yield assert(patches(0))(isSubtype[DIDStatePatch.AddPublicKey](anything)) &&
        assert(patches(1))(isSubtype[DIDStatePatch.AddService](anything)) &&
        assert(patches(2))(equalTo(DIDStatePatch.RemovePublicKey("key-99"))) &&
        assert(patches(3))(equalTo(DIDStatePatch.RemoveService("service-99")))
    },
    test("keep track only the last key-pair for AddPublicKey patches on the same key id") {
      val curve = EllipticCurve.SECP256K1
      val createTemplate = generateDIDCreateTemplate()
      val updateTemplate = generateDIDUpdateTemplate(patches =
        (1 to 10).map(_ =>
          ManagedDIDUpdatePatch.AddPublicKey(
            DIDPublicKeyTemplate(
              id = "key-1",
              purpose = VerificationRelationship.Authentication
            )
          )
        )
      )
      for {
        svc <- ZIO.service[ManagedDIDService]
        testDIDSvc <- ZIO.service[TestDIDService]
        did <- svc.createAndStoreDID(createTemplate).map(_.toCanonical)
        _ <- svc.publishStoredDID(did)
        _ <- svc.updateDIDAndPublish(did, updateTemplate)
        // compare that publicKeys are the same
        publishedUpdateOps <- testDIDSvc.getPublishedUpdateOperations
        (x1, y1) = publishedUpdateOps.head.delta.patches
          .collect { case p: DIDStatePatch.AddPublicKey => p }
          .last
          .publicKey match {
          case PublicKey.JsonWebKey2020(_, _, PublicKeyJwk.ECPublicKeyData(_, x, y)) => (x.toByteArray, y.toByteArray)
        }
        keyPair <- svc.secretStorage.getKey(did, "key-1").map(_.get.publicKey.p)
        (x2, y2) = (keyPair.x.toPaddedByteArray(curve), keyPair.y.toPaddedByteArray(curve))
      } yield assert(x1)(equalTo(x2)) && assert(y1)(equalTo(y2))
    },
    test("reject update if pending staging update exists") {
      val createTemplate = generateDIDCreateTemplate()
      val updateTemplate1 = generateDIDUpdateTemplate()
      val updateTemplate2 = generateDIDUpdateTemplate()
      val effect = for {
        svc <- ZIO.service[ManagedDIDService]
        testDIDSvc <- ZIO.service[TestDIDService]
        did <- svc.createAndStoreDID(createTemplate).map(_.toCanonical)
        _ <- svc.publishStoredDID(did)
        _ <- svc.updateDIDAndPublish(did, updateTemplate1)
        // inject operation to staging secret to make next update fail
        operation <- testDIDSvc.getPublishedUpdateOperations.map(_.head)
        keyPair <- KeyGeneratorWrapper.generateECKeyPair(EllipticCurve.SECP256K1)
        _ <- svc.secretStorage.setStagingDIDUpdateSecret(
          did,
          StagingDIDUpdateSecret(
            operation = operation,
            updateCommitmentSecret = keyPair,
            keyPairs = Map.empty
          )
        )
        _ <- svc.updateDIDAndPublish(did, updateTemplate2)
      } yield ()
      assertZIO(effect.exit)(fails(isSubtype[UpdateManagedDIDError.PendingStagingUpdate](anything)))
    } @@ TestAspect.tag("dev"),
    test("reject update if DID is not published") {
      val did = PrismDIDV1.fromCreateOperation(
        PublishedDIDOperation.Create(
          updateCommitment = HexString.fromStringUnsafe("00"),
          recoveryCommitment = HexString.fromStringUnsafe("00"),
          storage = DIDStorage.Cardano("testnet"),
          document = DIDDocument(Nil, Nil)
        )
      )
      val updateTemplate = generateDIDUpdateTemplate()
      val result = ZIO.serviceWithZIO[ManagedDIDService](_.updateDIDAndPublish(did, updateTemplate))
      assertZIO(result.exit)(fails(isSubtype[UpdateManagedDIDError.DIDNotPublished](anything)))
    }
  )

}
