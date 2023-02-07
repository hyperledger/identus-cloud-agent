package io.iohk.atala.agent.walletapi.storage

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.agent.walletapi.crypto.KeyGeneratorWrapper
import io.iohk.atala.agent.walletapi.model.{
  DIDPublicKeyTemplate,
  DIDUpdateLineage,
  ECKeyPair,
  ManagedDIDState,
  ManagedDIDTemplate
}
import io.iohk.atala.agent.walletapi.sql.{JdbcDIDNonSecretStorage, JdbcDIDSecretStorage}
import io.iohk.atala.agent.walletapi.util.OperationFactory
import io.iohk.atala.castor.core.model.did.{
  EllipticCurve,
  PrismDID,
  PrismDIDOperation,
  ScheduledDIDOperationStatus,
  VerificationRelationship
}
import io.iohk.atala.test.container.{DBTestUtils, PostgresTestContainerSupport}

import java.time.Instant
import scala.collection.immutable.ArraySeq
import org.postgresql.util.PSQLException

object JdbcDIDSecretStorageSpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  private val didExample = PrismDID.buildLongFormFromOperation(PrismDIDOperation.Create(Nil, Nil, Nil))

  private def updateLineage(
      operationId: Array[Byte] = Array.fill(32)(0),
      operationHash: Array[Byte] = Array.fill(32)(0),
      status: ScheduledDIDOperationStatus = ScheduledDIDOperationStatus.Confirmed
  ) = DIDUpdateLineage(
    operationId = ArraySeq.from(operationId),
    operationHash = ArraySeq.from(operationHash),
    previousOperationHash = ArraySeq.fill(32)(0),
    status = status,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH
  )

  private def generateKeyPair() = KeyGeneratorWrapper.generateECKeyPair(EllipticCurve.SECP256K1)

  private def generateCreateOperation(keyIds: Seq[String]) =
    OperationFactory.makeCreateOperation("master0", EllipticCurve.SECP256K1, generateKeyPair)(
      ManagedDIDTemplate(
        publicKeys = keyIds.map(DIDPublicKeyTemplate(_, VerificationRelationship.Authentication)),
        services = Nil
      )
    )

  private def initializeDIDStateAndKeys(keyIds: Seq[String] = Nil) = {
    for {
      nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
      secretStorage <- ZIO.service[DIDSecretStorage]
      generated <- generateCreateOperation(keyIds)
      (createOperation, secrets) = generated
      did = createOperation.did
      keyPairs = secrets.keyPairs.toSeq
      _ <- nonSecretStorage.setManagedDIDState(did, ManagedDIDState.Created(createOperation))
      _ <- ZIO.foreach(keyPairs) { case (keyId, keyPair) =>
        secretStorage.insertKey(did, keyId, keyPair, createOperation.toAtalaOperationHash)
      }
    } yield (did, keyPairs)
  }

  override def spec = {
    val testSuite =
      suite("JdbcDIDSecretStorageSpec")(
        listKeySpec,
        getKeySpec,
        insertKeySpec,
        removeKeySpec
      ) @@ TestAspect.sequential @@ TestAspect.before(DBTestUtils.runMigrationAgentDB) @@ TestAspect.timed @@ TestAspect
        .tag("dev")

    testSuite.provideSomeLayer(
      pgContainerLayer >+> transactorLayer >+> (JdbcDIDSecretStorage.layer ++ JdbcDIDNonSecretStorage.layer)
    )
  }

  private val listKeySpec = suite("listKeys")(
    test("initialize with empty list") {
      for {
        storage <- ZIO.service[DIDSecretStorage]
        keys <- storage.listKeys(didExample)
      } yield assert(keys)(isEmpty)
    },
    test("list all existing keys") {
      val operationHash = Array.fill[Byte](32)(0)
      for {
        storage <- ZIO.service[DIDSecretStorage]
        keyPairs <- ZIO.foreach(Seq("key-1", "key-2", "key-3"))(keyId => generateKeyPair().map(keyId -> _))
        _ <- ZIO.foreachDiscard(keyPairs) { case (keyId, keyPair) =>
          storage.insertKey(didExample, keyId, keyPair, operationHash)
        }
        readKeyPairs <- storage.listKeys(didExample).map(_.map(i => i._1 -> i._3))
      } yield assert(readKeyPairs)(hasSameElements(keyPairs))
    }
  )

  private val getKeySpec = suite("getKey")(
    test("return None if key doesn't exist") {
      for {
        storage <- ZIO.service[DIDSecretStorage]
        readKeyPair <- storage.getKey(didExample, "key-1")
      } yield assert(readKeyPair)(isNone)
    },
    test("return None if key exists but is not part of CreateOperation or confirmed UpdateOperation") {
      val operationHash = Array.fill[Byte](32)(0)
      for {
        storage <- ZIO.service[DIDSecretStorage]
        keyPair <- generateKeyPair()
        _ <- storage.insertKey(didExample, "key-1", keyPair, operationHash)
        readKeyPair <- storage.getKey(didExample, "key-1")
      } yield assert(readKeyPair)(isNone)
    },
    test("return key if exists and is part of CreateOperation") {
      for {
        storage <- ZIO.service[DIDSecretStorage]
        generated <- initializeDIDStateAndKeys(Seq("key-1"))
        (did, keyPairs) = generated
        readKeyPair <- storage.getKey(did, "key-1")
      } yield assert(readKeyPair)(isSome(equalTo(keyPairs.head._2)))
    },
    test("return key if exists and is part of confirmed UpdateOperation") {
      val operationHash = Array.fill[Byte](32)(42)
      for {
        storage <- ZIO.service[DIDSecretStorage]
        nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
        generated <- initializeDIDStateAndKeys()
        (did, _) = generated
        _ <- nonSecretStorage.insertDIDUpdateLineage(
          did,
          updateLineage(operationHash = operationHash, status = ScheduledDIDOperationStatus.Confirmed)
        )
        keyPair <- generateKeyPair()
        _ <- storage.insertKey(did, "key-1", keyPair, operationHash)
        readKeyPair <- storage.getKey(did, "key-1")
      } yield assert(readKeyPair)(isSome(equalTo(keyPair)))
    },
    test("return None if key exists and is part of unconfirmed UpdateOperation") {
      val inputs = Seq(
        ("key-1", Array.fill[Byte](32)(1), ScheduledDIDOperationStatus.Pending),
        ("key-2", Array.fill[Byte](32)(2), ScheduledDIDOperationStatus.AwaitingConfirmation),
        ("key-3", Array.fill[Byte](32)(3), ScheduledDIDOperationStatus.Rejected),
      )
      for {
        storage <- ZIO.service[DIDSecretStorage]
        nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
        generated <- initializeDIDStateAndKeys()
        (did, _) = generated
        keyIds <- ZIO.foreach(inputs) { case (keyId, hash, status) =>
          for {
            _ <- nonSecretStorage.insertDIDUpdateLineage(
              did,
              updateLineage(
                operationId = hash,
                operationHash = hash,
                status = status
              )
            )
            keyPair <- generateKeyPair()
            _ <- storage.insertKey(did, keyId, keyPair, hash)
          } yield keyId
        }
        readKeyPairs <- ZIO.foreach(keyIds)(keyId => storage.getKey(did, keyId))
      } yield assert(readKeyPairs)(forall(isNone))
    }
  )

  private val insertKeySpec = suite("insertKey")(
    test("insert new key") {
      for {
        storage <- ZIO.service[DIDSecretStorage]
        keyPair <- generateKeyPair()
        row <- storage.insertKey(didExample, "key-1", keyPair, Array.fill(32)(0))
        readKeyPairs <- storage.listKeys(didExample).map(_.map(i => i._1 -> i._3))
      } yield assert(readKeyPairs)(contains(("key-1", keyPair))) && assert(row)(equalTo(1))
    },
    test("insert same key-id with different did") {
      for {
        storage <- ZIO.service[DIDSecretStorage]
        did1 = PrismDID.buildCanonicalFromSuffix("0" * 64).toOption.get
        did2 = PrismDID.buildCanonicalFromSuffix("1" * 64).toOption.get
        keyPair1 <- generateKeyPair()
        keyPair2 <- generateKeyPair()
        row1 <- storage.insertKey(did1, "key-1", keyPair1, Array.fill(32)(0))
        row2 <- storage.insertKey(did2, "key-1", keyPair2, Array.fill(32)(0))
        readKeyPairs1 <- storage.listKeys(did1).map(_.map(i => i._1 -> i._3))
        readKeyPairs2 <- storage.listKeys(did2).map(_.map(i => i._1 -> i._3))
      } yield assert(readKeyPairs1)(contains(("key-1" -> keyPair1))) && assert(readKeyPairs2)(
        contains(("key-1" -> keyPair2))
      ) && assert(row1)(equalTo(1)) && assert(row2)(equalTo(1))
    },
    test("insert same key-id, did with different opration hash") {
      for {
        storage <- ZIO.service[DIDSecretStorage]
        rows <- ZIO.foreach(1 to 10) { i =>
          for {
            keyPair <- generateKeyPair()
            row <- storage.insertKey(didExample, s"key-$i", keyPair, Array.fill(32)(i.toByte))
          } yield row
        }
        readKeyPairs <- storage.listKeys(didExample)
      } yield assert(readKeyPairs)(hasSize(equalTo(10))) && assert(rows)(forall(equalTo(1)))
    },
    test("fail on inserting same key-id, did, operation hash") {
      val effect = for {
        storage <- ZIO.service[DIDSecretStorage]
        keyPair1 <- generateKeyPair()
        keyPair2 <- generateKeyPair()
        _ <- storage.insertKey(didExample, "key-1", keyPair1, Array.fill(32)(0))
        _ <- storage.insertKey(didExample, "key-1", keyPair2, Array.fill(32)(0))
      } yield ()

      assertZIO(effect.exit)(
        fails(
          isSubtype[PSQLException](
            hasField("getMessage", _.getMessage(), containsString("duplicate key value violates unique constraint"))
          )
        )
      )
    }
  )

  private val removeKeySpec = suite("removeKey")(
    test("do not fail on removing non-existing keys") {
      for {
        storage <- ZIO.service[DIDSecretStorage]
        n <- storage.removeKey(didExample, "key-1")
      } yield assert(n)(isZero)
    },
    test("remove exiting keys") {
      for {
        storage <- ZIO.service[DIDSecretStorage]
        keyPair <- generateKeyPair()
        _ <- storage.insertKey(didExample, "key-1", keyPair, Array.fill(32)(0))
        _ <- storage.insertKey(didExample, "key-1", keyPair, Array.fill(32)(1))
        _ <- storage.insertKey(didExample, "key-1", keyPair, Array.fill(32)(2))
        _ <- storage.insertKey(didExample, "key-2", keyPair, Array.fill(32)(3))
        before <- storage.listKeys(didExample)
        n <- storage.removeKey(didExample, "key-1")
        after <- storage.listKeys(didExample)
      } yield assert(before)(hasSize(equalTo(4))) && assert(after.map(_._1))(hasSameElements(Seq("key-2"))) && assert(n)(equalTo(3))
    }
  )

}
