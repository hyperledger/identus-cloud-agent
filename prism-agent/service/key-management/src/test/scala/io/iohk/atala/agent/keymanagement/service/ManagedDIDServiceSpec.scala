package io.iohk.atala.agent.keymanagement.service

import io.iohk.atala.agent.keymanagement.model.error.{CreateManagedDIDError, PublishManagedDIDError}
import io.iohk.atala.agent.keymanagement.model.{CommitmentPurpose, DIDPublicKeyTemplate, ManagedDIDTemplate}
import io.iohk.atala.castor.core.model.did.{
  DIDDocument,
  DIDStorage,
  LongFormPrismDIDV1,
  PrismDIDV1,
  PublishedDIDOperation,
  PublishedDIDOperationOutcome,
  Service,
  VerificationRelationship
}
import io.iohk.atala.castor.core.model.error
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*
import zio.test.*
import zio.test.Assertion.*

object ManagedDIDServiceSpec extends ZIOSpecDefault {

  private trait TestDIDService extends DIDService {
    def getPublishedOperations: UIO[Seq[PublishedDIDOperation.Create]]
  }

  private def testDIDServiceLayer = ZLayer.fromZIO {
    Ref.make(Seq.empty[PublishedDIDOperation.Create]).map { store =>
      new TestDIDService {
        override def createPublishedDID(
            operation: PublishedDIDOperation.Create
        ): IO[error.DIDOperationError, PublishedDIDOperationOutcome] =
          store
            .update(_.appended(operation))
            .as(
              PublishedDIDOperationOutcome(
                PrismDIDV1.fromCreateOperation(operation),
                operation,
                HexString.fromStringUnsafe("00")
              )
            )

        override def getPublishedOperations: UIO[Seq[PublishedDIDOperation.Create]] = store.get
      }
    }
  }

  private def managedDIDServiceLayer: ULayer[TestDIDService & ManagedDIDService] = {
    val didValidatorConfig = DIDOperationValidator.Config(50, 50)
    (DIDOperationValidator.layer(didValidatorConfig) ++ testDIDServiceLayer) >+> ManagedDIDService.inMemoryStorage()
  }

  private def generateDIDTemplate(
      publicKeys: Seq[DIDPublicKeyTemplate] = Nil,
      services: Seq[Service] = Nil
  ): ManagedDIDTemplate = ManagedDIDTemplate("testnet", publicKeys, services)

  override def spec =
    suite("ManagedDIDService")(publishStoredDIDSpec, createAndStoreDIDSpec).provideLayer(managedDIDServiceLayer)

  private val publishStoredDIDSpec =
    suite("publishStoredDID")(
      test("publish stored DID if exists") {
        val template = generateDIDTemplate()
        for {
          svc <- ZIO.service[ManagedDIDService]
          testDIDSvc <- ZIO.service[TestDIDService]
          did <- svc.createAndStoreDID(template).map(_.toCanonical)
          createOp <- svc.nonSecretStorage.getCreatedDID(did)
          opsBefore <- testDIDSvc.getPublishedOperations
          _ <- svc.publishStoredDID(did)
          opsAfter <- testDIDSvc.getPublishedOperations
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
        val template = generateDIDTemplate()
        for {
          svc <- ZIO.service[ManagedDIDService]
          testDIDSvc <- ZIO.service[TestDIDService]
          longFormDID <- svc.createAndStoreDID(template)
          did = longFormDID.toCanonical
          createOp <- svc.nonSecretStorage.getCreatedDID(did)
          _ <- svc.publishStoredDID(longFormDID)
          opsAfter <- testDIDSvc.getPublishedOperations
        } yield assert(opsAfter)(hasSameElements(createOp.toList))
      }
    )

  private val createAndStoreDIDSpec = suite("createAndStoreDID")(
    test("create and store DID list in DIDNonSecretStorage") {
      val template = generateDIDTemplate()
      for {
        svc <- ZIO.service[ManagedDIDService]
        didsBefore <- svc.nonSecretStorage.listCreatedDID
        did <- svc.createAndStoreDID(template).map(_.toCanonical)
        didsAfter <- svc.nonSecretStorage.listCreatedDID
      } yield assert(didsBefore)(isEmpty) &&
        assert(didsAfter)(hasSameElements(Seq(did)))
    },
    test("create and store DID secret in DIDSecretStorage") {
      val template = generateDIDTemplate(
        publicKeys = Seq(
          DIDPublicKeyTemplate("key-1", VerificationRelationship.Authentication),
          DIDPublicKeyTemplate("key-2", VerificationRelationship.KeyAgreement)
        )
      )
      for {
        svc <- ZIO.service[ManagedDIDService]
        did <- svc.createAndStoreDID(template).map(_.toCanonical)
        updateCommitment <- svc.secretStorage.getDIDCommitmentRevealValue(did, CommitmentPurpose.Update)
        recoveryCommitment <- svc.secretStorage.getDIDCommitmentRevealValue(did, CommitmentPurpose.Recovery)
        keyPairs <- svc.secretStorage.listKeys(did)
      } yield assert(updateCommitment)(isSome) &&
        assert(recoveryCommitment)(isSome) &&
        assert(keyPairs.keys)(hasSameElements(Seq("key-1", "key-2")))
    },
    test("created DID have corresponding public keys in CreateOperation") {
      val template = generateDIDTemplate(
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
      val template = generateDIDTemplate(
        publicKeys = Seq(
          DIDPublicKeyTemplate("key-1", VerificationRelationship.Authentication),
          DIDPublicKeyTemplate("key-1", VerificationRelationship.KeyAgreement)
        )
      )
      val result = ZIO.serviceWithZIO[ManagedDIDService](_.createAndStoreDID(template))
      assertZIO(result.exit)(fails(isSubtype[CreateManagedDIDError.OperationError](anything)))
    }
  )

}
