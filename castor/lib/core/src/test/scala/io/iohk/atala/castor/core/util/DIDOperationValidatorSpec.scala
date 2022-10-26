package io.iohk.atala.castor.core.util

import java.net.URI
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.castor.core.model.did.{
  DIDDocument,
  DIDStatePatch,
  DIDStorage,
  EllipticCurve,
  PrismDIDV1,
  PublicKey,
  PublicKeyJwk,
  PublishedDIDOperation,
  Service,
  ServiceType,
  UpdateOperationDelta
}
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.util.DIDOperationValidator.Config
import zio.*
import zio.test.*
import zio.test.Assertion.*

object DIDOperationValidatorSpec extends ZIOSpecDefault {

  private def generateCreateDIDOperation(
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

  private def generateUpdateDIDOperation(
      updateCommitment: HexString = HexString.fromStringUnsafe("0" * 64),
      patches: Seq[DIDStatePatch] = Nil
  ) = PublishedDIDOperation.Update(
    did = PrismDIDV1.fromCreateOperation(generateCreateDIDOperation()),
    updateKey = Base64UrlString.fromStringUnsafe("0"),
    previousVersion = HexString.fromStringUnsafe("0" * 64),
    delta = UpdateOperationDelta(
      patches = patches,
      updateCommitment = updateCommitment
    ),
    signature = Base64UrlString.fromStringUnsafe("0")
  )

  private def generatePublicKey(id: String): PublicKey = PublicKey.JsonWebKey2020(
    id = id,
    purposes = Nil,
    publicKeyJwk = PublicKeyJwk.ECPublicKeyData(
      crv = EllipticCurve.SECP256K1,
      x = Base64UrlString.fromStringUnsafe("00"),
      y = Base64UrlString.fromStringUnsafe("00")
    )
  )

  private val defaultDIDOpValidator = DIDOperationValidator(Config(50, 50))

  override def spec =
    suite("DIDOperationValidator")(createDIDSuite, updateDIDSuite) @@ TestAspect.samples(20)

  private val createDIDSuite =
    suite("CreatePublishedDID validation")(
      test("accept valid CreateOperation") {
        val op = generateCreateDIDOperation()
        assert(defaultDIDOpValidator.validate(op))(isRight)
      },
      test("reject CreateOperation on invalid updateCommitment length") {
        val updateCommitmentGen = Gen
          .stringBounded(0, 100)(Gen.hexCharLower)
          .filter(_.length % 2 == 0)
          .filter(_.length != 64)
        val recoveryCommitmentGen = Gen.stringN(64)(Gen.hexCharLower)
        check(updateCommitmentGen, recoveryCommitmentGen) { (u, r) =>
          val op = generateCreateDIDOperation(
            updateCommitment = HexString.fromStringUnsafe(u),
            recoveryCommitment = HexString.fromStringUnsafe(r)
          )
          assert(defaultDIDOpValidator.validate(op))(
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
          val op = generateCreateDIDOperation(
            updateCommitment = HexString.fromStringUnsafe(u),
            recoveryCommitment = HexString.fromStringUnsafe(r)
          )
          assert(defaultDIDOpValidator.validate(op))(
            isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
          )
        }
      },
      test("reject CreateOperation on too many DID publicKey access") {
        val keyLimitGen = Gen.int(0, 30)
        val keyCountGen = Gen.int(0, 30)
        check(keyLimitGen, keyCountGen) { (keyLimit, keyCount) =>
          val publicKeys = (1 to keyCount).map(i => generatePublicKey(s"key-$i"))
          val op = generateCreateDIDOperation(publicKeys = publicKeys)
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
              id = s"service-$i",
              `type` = ServiceType.MediatorService,
              serviceEndpoint = URI.create("https://example.com")
            )
          )
          val op = generateCreateDIDOperation(services = services)
          val expect =
            if (serviceCount <= serviceLimit) isRight
            else isLeft(isSubtype[DIDOperationError.TooManyDidServiceAccess](anything))
          assert(DIDOperationValidator(Config(0, serviceLimit)).validate(op))(expect)
        }
      },
      test("reject CreateOperation on duplicated DID public key id") {
        val publicKeys = Seq("key-1", "key-2", "key-1").map(generatePublicKey)
        val op = generateCreateDIDOperation(publicKeys = publicKeys)
        assert(defaultDIDOpValidator.validate(op))(
          isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
        )
      },
      test("reject CreateOperation on duplicated DID service id") {
        val services = Seq("service-1", "service-2", "service-1").map(id =>
          Service(
            id = id,
            `type` = ServiceType.MediatorService,
            serviceEndpoint = URI.create("https://example.com")
          )
        )
        val op = generateCreateDIDOperation(services = services)
        assert(defaultDIDOpValidator.validate(op))(
          isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
        )
      }
    )

  private val updateDIDSuite =
    suite("UpdatePublishedDID validation")(
      test("accept valid UpdateOperatoin") {
        val op = generateUpdateDIDOperation()
        assert(defaultDIDOpValidator.validate(op))(isRight)
      },
      test("reject UpdateOperation on invalid updateCommitment length") {
        val updateCommitmentGen = Gen
          .stringBounded(0, 100)(Gen.hexCharLower)
          .filter(_.length % 2 == 0)
          .filter(_.length != 64)
        check(updateCommitmentGen) { u =>
          val op = generateUpdateDIDOperation(
            updateCommitment = HexString.fromStringUnsafe(u)
          )
          assert(defaultDIDOpValidator.validate(op))(
            isLeft(isSubtype[DIDOperationError.InvalidArgument](anything))
          )
        }
      },
      test("reject UpdateOperation on too many DID publicKey access") {
        val keyLimit = 30
        val addKeyGen = Gen.int(0, 30)
        val removeKeyGen = Gen.int(0, 30)
        check(addKeyGen, removeKeyGen) { (addKeyCount, removeKeyCount) =>
          val addKeyPatch = (1 to addKeyCount).map(i => DIDStatePatch.AddPublicKey(generatePublicKey(s"key-$i")))
          val removeKeyPatch = (1 to removeKeyCount).map(i => DIDStatePatch.RemovePublicKey(s"remove-$i"))
          val op = generateUpdateDIDOperation(patches = addKeyPatch ++ removeKeyPatch)
          val expect =
            if (addKeyCount + removeKeyCount <= keyLimit) isRight
            else isLeft(isSubtype[DIDOperationError.TooManyDidPublicKeyAccess](anything))
          assert(DIDOperationValidator(Config(keyLimit, 0)).validate(op))(expect)
        }
      },
      test("reject CreateOperation on too many DID service access") {
        val serviceLimit = 30
        val addServiceGen = Gen.int(0, 30)
        val removeServiceGen = Gen.int(0, 30)
        check(addServiceGen, removeServiceGen) { (addServiceCount, removeServiceCount) =>
          val addServicePatch = (1 to addServiceCount).map(i =>
            DIDStatePatch.AddService(
              Service(
                id = s"service-$i",
                `type` = ServiceType.MediatorService,
                serviceEndpoint = URI.create("https://example.com")
              )
            )
          )
          val removeServicePatch = (1 to removeServiceCount).map(i => DIDStatePatch.RemoveService(s"remove-$i"))
          val op = generateUpdateDIDOperation(patches = addServicePatch ++ removeServicePatch)
          val expect =
            if (addServiceCount + removeServiceCount <= serviceLimit) isRight
            else isLeft(isSubtype[DIDOperationError.TooManyDidServiceAccess](anything))
          assert(DIDOperationValidator(Config(0, serviceLimit)).validate(op))(expect)
        }
      }
    )
}
