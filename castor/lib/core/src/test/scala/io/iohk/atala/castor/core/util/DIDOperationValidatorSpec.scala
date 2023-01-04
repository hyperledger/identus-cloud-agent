package io.iohk.atala.castor.core.util

import java.net.{URI, URL}
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.castor.core.model.did.{
  EllipticCurve,
  InternalKeyPurpose,
  InternalPublicKey,
  PrismDID,
  PrismDIDOperation,
  PublicKey,
  PublicKeyData,
  Service,
  ServiceType,
  UpdateDIDAction,
  VerificationRelationship
}
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.util.DIDOperationValidator.Config
import zio.*
import zio.test.*
import zio.test.Assertion.*

import scala.collection.immutable.ArraySeq

object DIDOperationValidatorSpec extends ZIOSpecDefault {

  override def spec = suite("DIDOperationValidator")(createOperationValidationSpec, updateOperationValidationSpec)

  private val publicKeyData = PublicKeyData.ECKeyData(
    crv = EllipticCurve.SECP256K1,
    x = Base64UrlString.fromStringUnsafe("00"),
    y = Base64UrlString.fromStringUnsafe("00")
  )

  private val createOperationValidationSpec = {
    def createPrismDIDOperation(
        publicKeys: Seq[PublicKey] = Nil,
        internalKeys: Seq[InternalPublicKey] = Nil,
        services: Seq[Service] = Nil
    ) =
      PrismDIDOperation.Create(publicKeys = publicKeys, internalKeys = internalKeys, services = services)

    suite("CreateOperation validation")(
      test("accept valid CreateOperation") {
        val op = createPrismDIDOperation()
        assert(DIDOperationValidator(Config(50, 50)).validate(op))(isRight)
      },
      test("reject CreateOperation on too many DID publicKey access") {
        val publicKeys = (1 to 10).map(i =>
          PublicKey(
            id = s"key$i",
            purpose = VerificationRelationship.Authentication,
            publicKeyData = publicKeyData
          )
        )
        val internalKeys = (1 to 10).map(i =>
          InternalPublicKey(
            id = s"master$i",
            purpose = InternalKeyPurpose.Master,
            publicKeyData = publicKeyData
          )
        )
        val op = createPrismDIDOperation(publicKeys = publicKeys, internalKeys = internalKeys)
        assert(DIDOperationValidator(Config(15, 15)).validate(op))(
          isLeft(isSubtype[DIDOperationError.TooManyDidPublicKeyAccess](anything))
        )
      },
      test("reject CreateOperation on duplicated DID public key id") {
        val publicKeys = (1 to 10).map(i =>
          PublicKey(
            id = s"key$i",
            purpose = VerificationRelationship.Authentication,
            publicKeyData = publicKeyData
          )
        )
        val internalKeys = Seq(
          InternalPublicKey(
            id = s"key1",
            purpose = InternalKeyPurpose.Master,
            publicKeyData = publicKeyData
          )
        )
        val op = createPrismDIDOperation(publicKeys = publicKeys, internalKeys = internalKeys)
        assert(DIDOperationValidator(Config(50, 50)).validate(op))(
          isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
        )
      },
      test("reject CreateOperation on too many service access") {
        val services = (1 to 20).map(i =>
          Service(
            id = s"service$i",
            `type` = ServiceType.MediatorService,
            serviceEndpoint = Seq(URI.create("http://example.com"))
          )
        )
        val op = createPrismDIDOperation(services = services)
        assert(DIDOperationValidator(Config(15, 15)).validate(op))(
          isLeft(isSubtype[DIDOperationError.TooManyDidServiceAccess](anything))
        )
      },
      test("reject CreateOperation on duplicated service id") {
        val services = (1 to 3).map(i =>
          Service(
            id = s"service0",
            `type` = ServiceType.MediatorService,
            serviceEndpoint = Seq(URI.create("http://example.com"))
          )
        )
        val op = createPrismDIDOperation(services = services)
        assert(DIDOperationValidator(Config(15, 15)).validate(op))(
          isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
        )
      },
      test("reject CreateOperation on invalid key-id") {
        val publicKeys = Seq(
          PublicKey(
            id = "key-01",
            purpose = VerificationRelationship.Authentication,
            publicKeyData = publicKeyData
          )
        )
        val op = createPrismDIDOperation(publicKeys = publicKeys)
        assert(DIDOperationValidator(Config(50, 50)).validate(op))(
          isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
        )
      },
      test("reject CreateOperation on non-unique serviceEndpoint URI") {
        val services = Seq(
          Service(
            id = s"service0",
            `type` = ServiceType.MediatorService,
            serviceEndpoint = Seq(URI.create("http://example.com"), URI.create("http://example.com"))
          )
        )
        val op = createPrismDIDOperation(services = services)
        assert(DIDOperationValidator(Config(50, 50)).validate(op))(
          isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
        )
      }
    )
  }

  private val updateOperationValidationSpec = {
    def updatePrismDIDOperation(
        actions: Seq[UpdateDIDAction] = Nil
    ) =
      PrismDIDOperation.Update(did = PrismDID.buildCanonicalFromSuffix("0" * 64).toOption.get, ArraySeq.empty, actions)

    suite("UpdateOperation validation")(
      test("accept valid UpdateOperation") {
        val op = updatePrismDIDOperation()
        assert(DIDOperationValidator(Config(50, 50)).validate(op))(isRight)
      },
      test("reject UpdateOperation on too many DID publicKey access") {
        val addKeyActions = (1 to 10).map(i =>
          UpdateDIDAction.AddKey(
            PublicKey(
              id = s"key$i",
              purpose = VerificationRelationship.Authentication,
              publicKeyData = publicKeyData
            )
          )
        )
        val addInternalKeyActions = (1 to 10).map(i =>
          UpdateDIDAction.AddInternalKey(
            InternalPublicKey(
              id = s"master$i",
              purpose = InternalKeyPurpose.Master,
              publicKeyData = publicKeyData
            )
          )
        )
        val removeKeyActions = (1 to 10).map(i => UpdateDIDAction.RemoveKey(s"remove$i"))
        val op = updatePrismDIDOperation(addKeyActions ++ addInternalKeyActions ++ removeKeyActions)
        assert(DIDOperationValidator(Config(25, 25)).validate(op))(
          isLeft(isSubtype[DIDOperationError.TooManyDidPublicKeyAccess](anything))
        )
      },
      test("reject UpdateOperation on too many service access") {
        val addServiceActions = (1 to 10).map(i =>
          UpdateDIDAction.AddService(
            Service(
              id = s"service$i",
              `type` = ServiceType.MediatorService,
              serviceEndpoint = Seq(URI.create("http://example.com"))
            )
          )
        )
        val removeServiceActions = (1 to 10).map(i => UpdateDIDAction.RemoveService(s"remove$i"))
        val updateServiceActions = (1 to 10).map(i =>
          UpdateDIDAction.UpdateService(s"update$i", ServiceType.MediatorService, Seq(URI.create("http://example.com")))
        )
        val op = updatePrismDIDOperation(addServiceActions ++ removeServiceActions ++ updateServiceActions)
        assert(DIDOperationValidator(Config(25, 25)).validate(op))(
          isLeft(isSubtype[DIDOperationError.TooManyDidServiceAccess](anything))
        )
      },
      test("reject UpdateOperation on invalid key-id") {
        val action = UpdateDIDAction.AddKey(
          PublicKey(
            id = "key-01",
            purpose = VerificationRelationship.Authentication,
            publicKeyData = publicKeyData
          )
        )
        val op = updatePrismDIDOperation(Seq(action))
        assert(DIDOperationValidator(Config(50, 50)).validate(op))(
          isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
        )
      },
      test("reject UpdateOperation on non-unique serviceEndpoint URI") {
        val actions = Seq(
          UpdateDIDAction.AddService(
            Service(
              id = s"service0",
              `type` = ServiceType.MediatorService,
              serviceEndpoint = Seq(URI.create("http://example.com"), URI.create("http://example.com"))
            )
          )
        )
        val op = updatePrismDIDOperation(actions)
        assert(DIDOperationValidator(Config(50, 50)).validate(op))(
          isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
        )
      }
    )
  }

}
