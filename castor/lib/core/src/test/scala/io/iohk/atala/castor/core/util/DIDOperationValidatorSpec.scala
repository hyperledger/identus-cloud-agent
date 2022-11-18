package io.iohk.atala.castor.core.util

import java.net.URI
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.castor.core.model.did.{
  InternalKeyPurpose,
  VerificationRelationship,
  EllipticCurve,
  InternalPublicKey,
  PrismDIDOperation,
  PublicKey,
  PublicKeyData
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
    def createPrismDIDOperation(publicKeys: Seq[PublicKey] = Nil, internalKeys: Seq[InternalPublicKey] = Nil) =
      PrismDIDOperation.Create(publicKeys = publicKeys, internalKeys = internalKeys)

    suite("PrismDID validation")(
      test("accept valid CreateOperation") {
        val op = createPrismDIDOperation()
        assert(DIDOperationValidator(Config(50)).validate(op))(isRight)
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
        println(DIDOperationValidator(Config(15)).validate(op))
        assert(DIDOperationValidator(Config(15)).validate(op))(
          isLeft(isSubtype[DIDOperationError.TooManyDidPublicKeyAccess](anything))
        )
      },
      test("reject CreateOperation on duplicated DID public key id") {
        val publicKeys = Seq("key-1", "key-2", "key-1").map(id =>
          PublicKey(
            id = id,
            purpose = VerificationRelationship.Authentication,
            publicKeyData = PublicKeyData.ECKeyData(
              crv = EllipticCurve.SECP256K1,
              x = Base64UrlString.fromStringUnsafe("00"),
              y = Base64UrlString.fromStringUnsafe("00")
            )
          )
        )
        val op = createPrismDIDOperation(publicKeys = publicKeys)
        assert(DIDOperationValidator(Config(50)).validate(op))(
          isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
        )
      }
    )
  }

}
