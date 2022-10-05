package io.iohk.atala.castor.core.util

import java.net.URI
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.castor.core.model.did.{
  DIDDocument,
  EllipticCurve,
  PublicKey,
  PublicKeyJwk,
  PublishedDIDOperation,
  DIDStorage,
  Service,
  ServiceType
}
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.util.DIDOperationValidator.Config
import zio.*
import zio.test.*
import zio.test.Assertion.*

object DIDOperationValidatorSpec extends ZIOSpecDefault {

  override def spec =
    suite("DIDOperationValidator")(publishedDIDSuite) @@ TestAspect.samples(20)

  private val publishedDIDSuite = {
    def createPublishedDIDOperation(
        updateCommitment: HexString = HexString.fromStringUnsafe("0" * 64),
        recoveryCommitment: HexString = HexString.fromStringUnsafe("0" * 64),
        publicKeys: Seq[PublicKey] = Nil,
        services: Seq[Service] = Nil
    ) =
      PublishedDIDOperation.Create(
        updateCommitment = updateCommitment,
        recoveryCommitment = recoveryCommitment,
        storage = DIDStorage.Cardano("testnet"),
        document = DIDDocument(
          publicKeys = publicKeys,
          services = services
        )
      )

    suite("PublishedDID validation")(
      test("accept valid CreateOperation") {
        val op = createPublishedDIDOperation()
        assert(DIDOperationValidator(Config(50, 50)).validate(op))(isRight)
      },
      test("reject CreateOperation on invalid updateCommitment length") {
        val updateCommitmentGen = Gen
          .stringBounded(0, 100)(Gen.hexCharLower)
          .filter(_.length % 2 == 0)
          .filter(_.length != 64)
        val recoveryCommitmentGen = Gen.stringN(64)(Gen.hexCharLower)
        check(updateCommitmentGen, recoveryCommitmentGen) { (u, r) =>
          val op = createPublishedDIDOperation(
            updateCommitment = HexString.fromString(u).get,
            recoveryCommitment = HexString.fromString(r).get
          )
          assert(DIDOperationValidator(Config(50, 50)).validate(op))(
            isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
          )
        }
      },
      test("reject CreateOperation on invalid recoveryCommitment length") {
        val updateCommitmentGen = Gen.stringN(64)(Gen.hexCharLower)
        val recoveryCommitmentGen = Gen
          .stringBounded(0, 100)(Gen.hexCharLower)
          .filter(_.length % 2 == 0)
          .filter(_.length != 64)
        check(updateCommitmentGen, recoveryCommitmentGen) { (u, r) =>
          val op = createPublishedDIDOperation(
            updateCommitment = HexString.fromString(u).get,
            recoveryCommitment = HexString.fromString(r).get
          )
          assert(DIDOperationValidator(Config(50, 50)).validate(op))(
            isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
          )
        }
      },
      test("reject CreateOperation on too many DID publicKey access") {
        val keyLimitGen = Gen.int(0, 30)
        val keyCountGen = Gen.int(0, 30)
        check(keyLimitGen, keyCountGen) { (keyLimit, keyCount) =>
          val publicKeys = (1 to keyCount).map(i =>
            PublicKey.JsonWebKey2020(
              id = s"did:example:123#key-$i",
              purposes = Nil,
              publicKeyJwk = PublicKeyJwk.ECPublicKeyData(
                crv = EllipticCurve.SECP256K1,
                x = Base64UrlString.fromStringUnsafe("00"),
                y = Base64UrlString.fromStringUnsafe("00")
              )
            )
          )
          val op = createPublishedDIDOperation(publicKeys = publicKeys)
          val expect =
            if (keyCount <= keyLimit) isRight
            else isLeft(isSubtype[DIDOperationError.TooManyDidPublicKeyAccess](anything))
          assert(DIDOperationValidator(Config(keyLimit, 0)).validate(op))(expect)
        }
      },
      test("reject CreateOperation on too many DID service access") {
        val serviceLimitGen = Gen.int(0, 30)
        val serviceCountGen = Gen.int(0, 30)
        check(serviceLimitGen, serviceCountGen) { (serviceLimit, serviceCount) =>
          val services = (1 to serviceCount).map(i =>
            Service(
              id = s"did:example:123#service-$i",
              `type` = ServiceType.MediatorService,
              serviceEndpoint = URI.create("https://example.com")
            )
          )
          val op = createPublishedDIDOperation(services = services)
          val expect =
            if (serviceCount <= serviceLimit) isRight
            else isLeft(isSubtype[DIDOperationError.TooManyDidServiceAccess](anything))
          assert(DIDOperationValidator(Config(0, serviceLimit)).validate(op))(expect)
        }
      }
    )
  }

}
