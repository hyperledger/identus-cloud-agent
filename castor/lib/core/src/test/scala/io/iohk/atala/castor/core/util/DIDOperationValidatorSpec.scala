package io.iohk.atala.castor.core.util

import java.net.{URI, URL}
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.castor.core.model.did.{
  EllipticCurve,
  InternalKeyPurpose,
  InternalPublicKey,
  PrismDIDOperation,
  PublicKey,
  PublicKeyData,
  Service,
  ServiceType,
  VerificationRelationship
}
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.util.DIDOperationValidator.Config
import zio.*
import zio.test.*
import zio.test.Assertion.*

object DIDOperationValidatorSpec extends ZIOSpecDefault {

  override def spec =
    suite("DIDOperationValidator")(prismDIDValidationSpec) @@ TestAspect.samples(20)

  private val prismDIDValidationSpec = {
    def createPrismDIDOperation(
        publicKeys: Seq[PublicKey] = Nil,
        internalKeys: Seq[InternalPublicKey] = Nil,
        services: Seq[Service] = Nil
    ) =
      PrismDIDOperation.Create(publicKeys = publicKeys, internalKeys = internalKeys, services = services)

    suite("PrismDID validation")(
      test("accept valid CreateOperation") {
        val op = createPrismDIDOperation()
        assert(DIDOperationValidator(Config(50, 50)).validate(op))(isRight)
      },
      test("reject CreateOperation on too many DID publicKey access") {
        val publicKeyData = PublicKeyData.ECKeyData(
          crv = EllipticCurve.SECP256K1,
          x = Base64UrlString.fromStringUnsafe("00"),
          y = Base64UrlString.fromStringUnsafe("00")
        )
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
        val publicKeyData = PublicKeyData.ECKeyData(
          crv = EllipticCurve.SECP256K1,
          x = Base64UrlString.fromStringUnsafe("00"),
          y = Base64UrlString.fromStringUnsafe("00")
        )
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
        val publicKeyData = PublicKeyData.ECKeyData(
          crv = EllipticCurve.SECP256K1,
          x = Base64UrlString.fromStringUnsafe("00"),
          y = Base64UrlString.fromStringUnsafe("00")
        )
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
      }
    )
  }

}
