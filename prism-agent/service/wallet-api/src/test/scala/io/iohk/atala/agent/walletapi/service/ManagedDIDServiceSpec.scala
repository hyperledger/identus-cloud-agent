package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.error.{CreateManagedDIDError, PublishManagedDIDError}
import io.iohk.atala.agent.walletapi.model.{DIDPublicKeyTemplate, ManagedDIDTemplate}
import io.iohk.atala.castor.core.model.did.{
  PrismDID,
  PrismDIDOperation,
  ScheduleDIDOperationOutcome,
  Service,
  SignedPrismDIDOperation,
  VerificationRelationship
}
import io.iohk.atala.castor.core.model.error
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*
import zio.test.*
import zio.test.Assertion.*

import scala.collection.immutable.ArraySeq

object ManagedDIDServiceSpec extends ZIOSpecDefault {

  private trait TestDIDService extends DIDService {
    def getPublishedOperations: UIO[Seq[SignedPrismDIDOperation.Create]]
  }

  private def testDIDServiceLayer = ZLayer.fromZIO {
    Ref.make(Seq.empty[SignedPrismDIDOperation.Create]).map { store =>
      new TestDIDService {
        override def createPublishedDID(
            signOperation: SignedPrismDIDOperation.Create
        ): IO[error.DIDOperationError, ScheduleDIDOperationOutcome] =
          store
            .update(_.appended(signOperation))
            .as(
              ScheduleDIDOperationOutcome(
                PrismDID.buildLongFormFromOperation(signOperation.operation).asCanonical,
                signOperation.operation,
                ArraySeq.empty
              )
            )

        override def getPublishedOperations: UIO[Seq[SignedPrismDIDOperation.Create]] = store.get
      }
    }
  }

  private def managedDIDServiceLayer: ULayer[TestDIDService & ManagedDIDService] =
    testDIDServiceLayer >+> ManagedDIDService.inMemoryStorage

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
          createOp <- svc.nonSecretStorage.getCreatedDID(did)
          opsBefore <- testDIDSvc.getPublishedOperations
          _ <- svc.publishStoredDID(did)
          opsAfter <- testDIDSvc.getPublishedOperations
        } yield assert(opsBefore)(isEmpty) &&
          assert(opsAfter)(hasSameElements(createOp.toList))
      },
      test("fail when publish non-existing DID") {
        val did = PrismDID.buildLongFormFromOperation(PrismDIDOperation.Create(Nil, Nil))
        val result = ZIO.serviceWithZIO[ManagedDIDService](_.publishStoredDID(did))
        assertZIO(result.exit)(fails(isSubtype[PublishManagedDIDError.DIDNotFound](anything)))
      },
      test("publish stored DID when provide long-form DID") {
        val template = generateDIDTemplate()
        for {
          svc <- ZIO.service[ManagedDIDService]
          testDIDSvc <- ZIO.service[TestDIDService]
          longFormDID <- svc.createAndStoreDID(template)
          did = longFormDID.asCanonical
          createOp <- svc.nonSecretStorage.getCreatedDID(did)
          _ <- svc.publishStoredDID(longFormDID)
          opsAfter <- testDIDSvc.getPublishedOperations
        } yield assert(opsAfter)(hasSameElements(createOp.toList))
      }
    ) @@ TestAspect.ignore // TODO: un-ignore

  private val createAndStoreDIDSpec = suite("createAndStoreDID")(
    test("create and store DID list in DIDNonSecretStorage") {
      val template = generateDIDTemplate()
      for {
        svc <- ZIO.service[ManagedDIDService]
        didsBefore <- svc.nonSecretStorage.listCreatedDID
        did <- svc.createAndStoreDID(template).map(_.asCanonical)
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
        did <- svc.createAndStoreDID(template).map(_.asCanonical)
        keyPairs <- svc.secretStorage.listKeys(did)
      } yield assert(keyPairs.keys)(hasSameElements(Seq("key-1", "key-2")))
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
        did <- svc.createAndStoreDID(template).map(_.asCanonical)
        createOperation <- svc.nonSecretStorage.getCreatedDID(did)
        publicKeys <- ZIO.fromOption(createOperation).map(_.publicKeys)
      } yield assert(publicKeys.map(i => i.id -> i.purpose))(
        hasSameElements(
          Seq(
            "key-1" -> VerificationRelationship.Authentication,
            "key-2" -> VerificationRelationship.KeyAgreement,
            "key-3" -> VerificationRelationship.AssertionMethod
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
