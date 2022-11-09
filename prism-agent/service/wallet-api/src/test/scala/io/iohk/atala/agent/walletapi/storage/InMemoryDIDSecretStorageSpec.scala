package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.crypto.KeyGeneratorWrapper
import io.iohk.atala.castor.core.model.did.{
  DIDDocument,
  DIDStatePatch,
  DIDStorage,
  EllipticCurve,
  PrismDIDV1,
  PublicKey,
  PublishedDIDOperation,
  Service,
  UpdateOperationDelta
}
import io.iohk.atala.agent.walletapi.model.*
import io.iohk.atala.agent.walletapi.model.ECCoordinates.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.models.HexStrings.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

import scala.collection.immutable.ArraySeq

object InMemoryDIDSecretStorageSpec extends ZIOSpecDefault {

  private val didExample = PrismDIDV1.fromCreateOperation(
    PublishedDIDOperation.Create(
      updateCommitment = HexString.fromStringUnsafe("00"),
      recoveryCommitment = HexString.fromStringUnsafe("00"),
      storage = DIDStorage.Cardano("testnet"),
      document = DIDDocument(publicKeys = Nil, services = Nil)
    )
  )

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
      patches: Seq[DIDStatePatch] = Nil,
      previousVersion: HexString = HexString.fromStringUnsafe("0" * 64)
  ) = PublishedDIDOperation.Update(
    did = PrismDIDV1.fromCreateOperation(generateCreateDIDOperation()),
    updateKey = Base64UrlString.fromStringUnsafe("0"),
    previousVersion = previousVersion,
    delta = UpdateOperationDelta(
      patches = patches,
      updateCommitment = updateCommitment
    ),
    signature = Base64UrlString.fromStringUnsafe("0")
  )

  private def generateKeyPair(publicKey: (Int, Int) = (0, 0), privateKey: ArraySeq[Byte] = ArraySeq(0)): ECKeyPair =
    ECKeyPair(
      publicKey = ECPublicKey(ECPoint(ECCoordinate.fromBigInt(publicKey._1), ECCoordinate.fromBigInt(publicKey._2))),
      privateKey = ECPrivateKey(privateKey)
    )

  override def spec = suite("InMemoryDIDSecretStorage")(
    listKeySpec,
    getKeySpec,
    upsertKeySpec,
    getDIDCommitmentSpec,
    upsertDIDCommitmentKeySpec,
    setStagingDIDUpdateSecretSpec
  ).provideLayer(InMemoryDIDSecretStorage.layer)

  private val listKeySpec = suite("listKeys")(
    test("initialize with empty list") {
      for {
        storage <- ZIO.service[DIDSecretStorage]
        keys <- storage.listKeys(didExample)
      } yield assert(keys)(isEmpty)
    },
    test("list all existing keys") {
      val keyPairs = Map(
        "key-1" -> generateKeyPair(publicKey = (1, 1)),
        "key-2" -> generateKeyPair(publicKey = (2, 2)),
        "key-3" -> generateKeyPair(publicKey = (3, 3))
      )
      for {
        storage <- ZIO.service[DIDSecretStorage]
        _ <- ZIO.foreachDiscard(keyPairs) { case (keyId, keyPair) =>
          storage.upsertKey(didExample, keyId, keyPair)
        }
        keys <- storage.listKeys(didExample)
      } yield assert(keys)(hasSameElements(keyPairs))
    }
  )

  private val getKeySpec = suite("getKey")(
    test("return stored key if exist") {
      val keyPair = generateKeyPair(publicKey = (1, 1))
      for {
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.upsertKey(didExample, "key-1", keyPair)
        key <- storage.getKey(didExample, "key-1")
      } yield assert(key)(isSome(equalTo(keyPair)))
    },
    test("return None if stored key doesn't exist") {
      val keyPair = generateKeyPair(publicKey = (1, 1))
      for {
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.upsertKey(didExample, "key-1", keyPair)
        key <- storage.getKey(didExample, "key-2")
      } yield assert(key)(isNone)
    }
  )

  private val upsertKeySpec = suite("upsertKey")(
    test("replace value for existing key") {
      val keyPair1 = generateKeyPair(publicKey = (1, 1))
      val keyPair2 = generateKeyPair(publicKey = (2, 2))
      for {
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.upsertKey(didExample, "key-1", keyPair1)
        _ <- storage.upsertKey(didExample, "key-1", keyPair2)
        key <- storage.getKey(didExample, "key-1")
      } yield assert(key)(isSome(equalTo(keyPair2)))
    }
  )

  private val getDIDCommitmentSpec = suite("getDIDCommitmentRevealValue")(
    test("get non-exists commitment reveal value") {
      for {
        storage <- ZIO.service[DIDSecretStorage]
        updateCommitment <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Update)
        recoveryCommitment <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Recovery)
      } yield assert(updateCommitment)(isNone) && assert(recoveryCommitment)(isNone)
    },
    test("get existing commit reveal value") {
      for {
        updateKeyPair <- KeyGeneratorWrapper.generateECKeyPair(EllipticCurve.SECP256K1)
        recoveryKeyPair <- KeyGeneratorWrapper.generateECKeyPair(EllipticCurve.SECP256K1)
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.upsertDIDCommitmentKey(didExample, CommitmentPurpose.Update, updateKeyPair)
        _ <- storage.upsertDIDCommitmentKey(didExample, CommitmentPurpose.Recovery, recoveryKeyPair)
        updateCommitment <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Update)
        recoveryCommitment <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Recovery)
      } yield assert(updateCommitment)(isSome(equalTo(updateKeyPair))) && assert(recoveryCommitment)(
        isSome(equalTo(recoveryKeyPair))
      )
    }
  )

  private val upsertDIDCommitmentKeySpec = suite("upsertDIDCommitmentKey")(
    test("insert non-existing commitment reveal value") {
      for {
        updateKeyPair <- KeyGeneratorWrapper.generateECKeyPair(EllipticCurve.SECP256K1)
        storage <- ZIO.service[DIDSecretStorage]
        before <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Update)
        _ <- storage.upsertDIDCommitmentKey(didExample, CommitmentPurpose.Update, updateKeyPair)
        after <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Update)
      } yield assert(before)(isNone) && assert(after)(isSome(equalTo(updateKeyPair)))
    },
    test("update existing commitment reveal value") {
      for {
        updateKeyPair1 <- KeyGeneratorWrapper.generateECKeyPair(EllipticCurve.SECP256K1)
        updateKeyPair2 <- KeyGeneratorWrapper.generateECKeyPair(EllipticCurve.SECP256K1)
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.upsertDIDCommitmentKey(didExample, CommitmentPurpose.Update, updateKeyPair1)
        before <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Update)
        _ <- storage.upsertDIDCommitmentKey(didExample, CommitmentPurpose.Update, updateKeyPair2)
        after <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Update)
      } yield assert(before)(isSome(equalTo(updateKeyPair1))) && assert(after)(isSome(equalTo(updateKeyPair2)))
    }
  )

  private val setStagingDIDUpdateSecretSpec = suite("setStagingDIDUpdateSecret")(
    test("succeeds when set staging secret") {
      val did = PrismDIDV1.fromCreateOperation(generateCreateDIDOperation())
      val stagingSecret = StagingDIDUpdateSecret(
        operation = generateUpdateDIDOperation(),
        updateCommitmentSecret = generateKeyPair(),
        keyPairs = Map.empty
      )
      for {
        storage <- ZIO.service[DIDSecretStorage]
        result <- storage.setStagingDIDUpdateSecret(did, stagingSecret)
        secret <- storage.getStagingDIDUpdateSecret(did)
      } yield assertTrue(result) && assert(secret)(isSome(equalTo(stagingSecret)))
    },
    test("fails when staging already exists and does not apply change") {
      val did = PrismDIDV1.fromCreateOperation(generateCreateDIDOperation())
      val stagingSecret1 = StagingDIDUpdateSecret(
        operation = generateUpdateDIDOperation(),
        updateCommitmentSecret = generateKeyPair(),
        keyPairs = Map.empty
      )
      val stagingSecret2 = stagingSecret1.copy(keyPairs = Map("key-1" -> generateKeyPair()))
      for {
        storage <- ZIO.service[DIDSecretStorage]
        result1 <- storage.setStagingDIDUpdateSecret(did, stagingSecret1)
        result2 <- storage.setStagingDIDUpdateSecret(did, stagingSecret2)
        secret <- storage.getStagingDIDUpdateSecret(did)
      } yield assertTrue(result1) && assert(result2)(isFalse) && assert(secret)(isSome(equalTo(stagingSecret1)))
    }
  )

}
