package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.castor.core.model.did.{DIDDocument, DIDStorage, PrismDIDV1, PublishedDIDOperation}
import io.iohk.atala.agent.walletapi.model.*
import io.iohk.atala.agent.walletapi.model.ECCoordinates.*
import io.iohk.atala.shared.models.HexStrings.HexString
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

  private def generateKeyPair(publicKey: (Int, Int) = (0, 0), privateKey: ArraySeq[Byte] = ArraySeq(0)): ECKeyPair =
    ECKeyPair(
      publicKey = ECPublicKey(ECPoint(ECCoordinate.fromBigInt(publicKey._1), ECCoordinate.fromBigInt(publicKey._2))),
      privateKey = ECPrivateKey(privateKey)
    )

  override def spec = suite("InMemoryDIDSecretStorage")(
    listKeySpec,
    getKeySpec,
    upsertKeySpec,
    removeKeySpec,
    getDIDCommitmentSpec,
    upsertDIDCommitmentRevealValue
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

  private val removeKeySpec = suite("removeKey")(
    test("remove existing key and return removed value") {
      val keyPair = generateKeyPair(publicKey = (1, 1))
      for {
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.upsertKey(didExample, "key-1", keyPair)
        _ <- storage.removeKey(didExample, "key-1")
        keys <- storage.listKeys(didExample)
      } yield assert(keys)(isEmpty)
    },
    test("remove non-existing key and return None for the removed value") {
      val keyPair = generateKeyPair(publicKey = (1, 1))
      for {
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.upsertKey(didExample, "key-1", keyPair)
        _ <- storage.removeKey(didExample, "key-2")
        keys <- storage.listKeys(didExample)
      } yield assert(keys)(hasSize(equalTo(1)))
    },
    test("remove some of existing keys and keep other keys") {
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
        _ <- storage.removeKey(didExample, "key-1")
        keys <- storage.listKeys(didExample)
      } yield assert(keys.keys)(hasSameElements(Seq("key-2", "key-3")))
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
      val updateHex = HexString.fromStringUnsafe("0011aabb")
      val recoveryHex = HexString.fromStringUnsafe("aabb0011")
      for {
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.upsertDIDCommitmentKey(didExample, CommitmentPurpose.Update, updateHex)
        _ <- storage.upsertDIDCommitmentKey(didExample, CommitmentPurpose.Recovery, recoveryHex)
        updateCommitment <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Update)
        recoveryCommitment <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Recovery)
      } yield assert(updateCommitment)(isSome(equalTo(updateHex))) && assert(recoveryCommitment)(
        isSome(equalTo(recoveryHex))
      )
    }
  )

  private val upsertDIDCommitmentRevealValue = suite("upsertDIDCommitmentRevealValue")(
    test("insert non-existing commitment reveal value") {
      val updateHex = HexString.fromStringUnsafe("0011aabb")
      for {
        storage <- ZIO.service[DIDSecretStorage]
        before <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Update)
        _ <- storage.upsertDIDCommitmentKey(didExample, CommitmentPurpose.Update, updateHex)
        after <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Update)
      } yield assert(before)(isNone) && assert(after)(isSome(equalTo(updateHex)))
    },
    test("update existing commitment reveal value") {
      val updateHex1 = HexString.fromStringUnsafe("0011aabb")
      val updateHex2 = HexString.fromStringUnsafe("aabb0011")
      for {
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.upsertDIDCommitmentKey(didExample, CommitmentPurpose.Update, updateHex1)
        before <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Update)
        _ <- storage.upsertDIDCommitmentKey(didExample, CommitmentPurpose.Update, updateHex2)
        after <- storage.getDIDCommitmentKey(didExample, CommitmentPurpose.Update)
      } yield assert(before)(isSome(equalTo(updateHex1))) && assert(after)(isSome(equalTo(updateHex2)))
    }
  )

}
