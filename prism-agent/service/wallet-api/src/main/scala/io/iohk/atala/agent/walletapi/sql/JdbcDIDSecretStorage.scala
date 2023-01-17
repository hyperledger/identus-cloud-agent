package io.iohk.atala.agent.walletapi.sql

import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage

import cats.instances.seq
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import java.time.Instant
import java.util.UUID

import zio.*
import zio.interop.catz.*
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError.KeyNotFoundError
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.castor.core.model.did.{PrismDID, EllipticCurve, ScheduledDIDOperationStatus}
import io.iohk.atala.agent.walletapi.model.ECKeyPair
import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.shared.utils.Base64Utils

class JdbcDIDSecretStorage(xa: Transactor[Task]) extends DIDSecretStorage {

  case class InstantAsBigInt(value: Instant)

  // Uncomment to have Doobie LogHandler in scope and automatically output SQL statements in logs
  // given logHandler: LogHandler = LogHandler.jdkLogHandler

  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  given uuidPut: Put[UUID] = Put[String].contramap(_.toString())

  given instantGet: Get[InstantAsBigInt] = Get[Long].map(Instant.ofEpochSecond).map(InstantAsBigInt.apply)
  given instantPut: Put[InstantAsBigInt] = Put[Long].contramap(_.value.getEpochSecond())

  given ecKeyPairPairGet: Get[ECKeyPair] = Get[String].map { b64 =>
    val bytes = Base64Utils.decodeURL(b64)
    val privateKey = EC.INSTANCE.toPrivateKeyFromBytes(bytes)
    val publicKey = EC.INSTANCE.toPublicKeyFromPrivateKey(privateKey)
    ECKeyPair.fromPrism14ECKeyPair(io.iohk.atala.prism.crypto.keys.ECKeyPair(publicKey, privateKey))
  }
  given ecKeyPairPut: Put[ECKeyPair] = Put[String].contramap { kp =>
    Base64Utils.encodeURL(kp.privateKey.toPaddedByteArray(EllipticCurve.SECP256K1))
  }

  given didIdGet: Get[DidId] = Get[String].map(DidId(_))
  given didIdPut: Put[DidId] = Put[String].contramap(_.value)
  given octetKeyPairGet: Get[OctetKeyPair] = Get[String].map(OctetKeyPair.parse)
  given octetKeyPairPut: Put[OctetKeyPair] = Put[String].contramap(_.toJSONString)

  override def removeDIDSecret(did: PrismDID): Task[Int] = {
    val cxnIO = sql"""
        | DELETE
        | FROM public.prism_did_secret_storage
        | WHERE
        |   did = $did
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def getKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]] = {
    // By specification, it is possible to have multiple unconfirmed operation_id with
    // the same operation_hash (same content different signature).
    // However, there can be only 1 confirmed operation_id per operation_hash.
    val status: ScheduledDIDOperationStatus = ScheduledDIDOperationStatus.Confirmed
    val cxnIO = sql"""
        | SELECT
        |   sc.key_pair
        | FROM public.prism_did_secret_storage sc
        |   JOIN public.prism_did_update_lineage ul ON sc.operation_hash = ul.operation_hash
        |   JOIN public.prism_did_wallet_state ws ON sc.did = ws.did
        | WHERE
        |   sc.did = $did
        |   AND sc.key_id = $keyId
        |   AND (ul.status = $status OR sc.operation_hash = sha256(ws.atala_operation_content))
        """.stripMargin
      .query[ECKeyPair]
      .option

    cxnIO.transact(xa)
  }

  override def insertKey(did: PrismDID, keyId: String, keyPair: ECKeyPair, operationHash: Array[Byte]): Task[Int] = {
    val cxnIO = (now: Instant) => sql"""
        | INSERT INTO public.prism_did_secret_storage(
        |   did,
        |   created_at,
        |   key_id,
        |   key_pair,
        |   operation_hash
        | ) values (
        |   ${did},
        |   ${now},
        |   ${keyId},
        |   ${keyPair},
        |   ${operationHash}
        | )
        """.stripMargin.update

    Clock.instant.flatMap(cxnIO(_).run.transact(xa)).tapErrorCause(e => ZIO.logErrorCause(e)) // TODO: remove
  }

  override def listKeys(did: PrismDID): Task[Map[String, ECKeyPair]] = {
    val cxnIO = sql"""
        | SELECT
        |   key_id,
        |   key_pair
        | FROM public.prism_did_secret_storage
        | WHERE
        |   did = $did
        """.stripMargin
      .query[(String, ECKeyPair)]
      .toMap

    cxnIO.transact(xa)
  }

  override def removeKey(did: PrismDID, keyId: String): Task[Int] = {
    val cxnIO = sql"""
        | DELETE
        | FROM public.prism_did_secret_storage
        | WHERE
        |   did = $did
        |   AND key_id = $keyId
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def getKey(did: DidId, keyId: String): Task[Option[OctetKeyPair]] = {
    val cxnIO = sql"""
        | SELECT
        |   key_pair
        | FROM public.did_secret_storage
        | WHERE
        |   did = $did
        |   AND key_id = $keyId
        """.stripMargin
      .query[OctetKeyPair]
      .option

    cxnIO.transact(xa)
  }

  override def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): Task[Int] = {
    val cxnIO = (now: InstantAsBigInt) => sql"""
        | INSERT INTO public.did_secret_storage(
        |   did,
        |   created_at,
        |   key_id,
        |   key_pair
        | ) values (
        |   ${did},
        |   ${now},
        |   ${keyId},
        |   ${keyPair}
        | )
        """.stripMargin.update

    Clock.instant.flatMap(i => cxnIO(InstantAsBigInt(i)).run.transact(xa))
  }

}

object JdbcDIDSecretStorage {
  val layer: URLayer[Transactor[Task], DIDSecretStorage] =
    ZLayer.fromFunction(new JdbcDIDSecretStorage(_))
}
