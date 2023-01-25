package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.castor.core.model.did.{PrismDID, PrismDIDOperation}
import io.iohk.atala.agent.walletapi.model.*
import io.iohk.atala.agent.walletapi.model.ECCoordinates.*
import io.iohk.atala.shared.models.HexStrings.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

import scala.collection.immutable.ArraySeq

object InMemoryDIDSecretStorageSpec extends ZIOSpecDefault {

  private val didExample = PrismDID.buildLongFormFromOperation(PrismDIDOperation.Create(Nil, Nil, Nil))

  private def generateKeyPair(publicKey: (Int, Int) = (0, 0), privateKey: ArraySeq[Byte] = ArraySeq(0)): ECKeyPair =
    ECKeyPair(
      publicKey = ECPublicKey(ECPoint(ECCoordinate.fromBigInt(publicKey._1), ECCoordinate.fromBigInt(publicKey._2))),
      privateKey = ECPrivateKey(privateKey)
    )

  override def spec = suite("InMemoryDIDSecretStorage")(
    listKeySpec,
    getKeySpec,
    upsertKeySpec,
    removeKeySpec
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
          storage.insertKey(didExample, keyId, keyPair, Array.empty)
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
        _ <- storage.insertKey(didExample, "key-1", keyPair, Array.empty)
        key <- storage.getKey(didExample, "key-1")
      } yield assert(key)(isSome(equalTo(keyPair)))
    },
    test("return None if stored key doesn't exist") {
      val keyPair = generateKeyPair(publicKey = (1, 1))
      for {
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.insertKey(didExample, "key-1", keyPair, Array.empty)
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
        _ <- storage.insertKey(didExample, "key-1", keyPair1, Array.empty)
        _ <- storage.insertKey(didExample, "key-1", keyPair2, Array.empty)
        key <- storage.getKey(didExample, "key-1")
      } yield assert(key)(isSome(equalTo(keyPair2)))
    }
  )

  private val removeKeySpec = suite("removeKey")(
    test("remove existing key and return removed value") {
      val keyPair = generateKeyPair(publicKey = (1, 1))
      for {
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.insertKey(didExample, "key-1", keyPair, Array.empty)
        _ <- storage.removeKey(didExample, "key-1")
        keys <- storage.listKeys(didExample)
      } yield assert(keys)(isEmpty)
    },
    test("remove non-existing key and return None for the removed value") {
      val keyPair = generateKeyPair(publicKey = (1, 1))
      for {
        storage <- ZIO.service[DIDSecretStorage]
        _ <- storage.insertKey(didExample, "key-1", keyPair, Array.empty)
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
          storage.insertKey(didExample, keyId, keyPair, Array.empty)
        }
        _ <- storage.removeKey(didExample, "key-1")
        keys <- storage.listKeys(didExample)
      } yield assert(keys.keys)(hasSameElements(Seq("key-2", "key-3")))
    }
  )
}
