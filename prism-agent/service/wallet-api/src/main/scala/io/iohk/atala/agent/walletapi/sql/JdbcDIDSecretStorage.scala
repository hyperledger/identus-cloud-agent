package io.iohk.atala.agent.walletapi.sql

import com.nimbusds.jose.jwk.OctetKeyPair
import doobie.*
import doobie.implicits.*
import io.iohk.atala.agent.walletapi.crypto.{ECKeyPair, Apollo}
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.utils.Base64Utils
import java.time.Instant
import java.util.UUID
import zio.*
import zio.interop.catz.*

class JdbcDIDSecretStorage(xa: Transactor[Task], apollo: Apollo) extends DIDSecretStorage {

  case class InstantAsBigInt(value: Instant)

  // Uncomment to have Doobie LogHandler in scope and automatically output SQL statements in logs
  // given logHandler: LogHandler = LogHandler.jdkLogHandler

  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  given uuidPut: Put[UUID] = Put[String].contramap(_.toString())

  given instantGet: Get[InstantAsBigInt] = Get[Long].map(Instant.ofEpochSecond).map(InstantAsBigInt.apply)
  given instantPut: Put[InstantAsBigInt] = Put[Long].contramap(_.value.getEpochSecond())

  // FIXME: this assumes that all ECKeyPair are secp256k1 curve
  given ecKeyPairPairGet: Get[ECKeyPair] = Get[String].map { b64 =>
    val bytes = Base64Utils.decodeURL(b64)
    val privateKey = apollo.ecKeyFactory.privateKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
    val publicKey = privateKey.computePublicKey
    ECKeyPair(publicKey, privateKey)
  }
  given ecKeyPairPut: Put[ECKeyPair] = Put[String].contramap { kp =>
    Base64Utils.encodeURL(kp.privateKey.encode)
  }

  given didIdGet: Get[DidId] = Get[String].map(DidId(_))
  given didIdPut: Put[DidId] = Put[String].contramap(_.value)
  given octetKeyPairGet: Get[OctetKeyPair] = Get[String].map(OctetKeyPair.parse)
  given octetKeyPairPut: Put[OctetKeyPair] = Put[String].contramap(_.toJSONString)

  // override def getKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]] = {
  //   // By specification, it is possible to have multiple unconfirmed operation_id with
  //   // the same operation_hash (same content different signature).
  //   // However, there can be only 1 confirmed operation_id per operation_hash.
  //   val status: ScheduledDIDOperationStatus = ScheduledDIDOperationStatus.Confirmed
  //   val cxnIO = sql"""
  //       | SELECT
  //       |   sc.key_pair
  //       | FROM public.prism_did_secret_storage sc
  //       |   LEFT JOIN public.prism_did_wallet_state ws ON sc.did = ws.did
  //       |   LEFT JOIN public.prism_did_update_lineage ul ON sc.operation_hash = ul.operation_hash
  //       | WHERE
  //       |   sc.did = $did
  //       |   AND sc.key_id = $keyId
  //       |   AND (ul.status = $status OR (ul.status IS NULL AND sc.operation_hash = sha256(ws.atala_operation_content)))
  //       """.stripMargin
  //     .query[ECKeyPair]
  //     .option

  //   cxnIO.transact(xa)
  // }

  // override def insertKey(did: PrismDID, keyId: String, keyPair: ECKeyPair, operationHash: Array[Byte]): Task[Int] = {
  //   val cxnIO = (now: Instant) => sql"""
  //       | INSERT INTO public.prism_did_secret_storage(
  //       |   did,
  //       |   created_at,
  //       |   key_id,
  //       |   key_pair,
  //       |   operation_hash
  //       | ) values (
  //       |   ${did},
  //       |   ${now},
  //       |   ${keyId},
  //       |   ${keyPair},
  //       |   ${operationHash}
  //       | )
  //       """.stripMargin.update

  //   Clock.instant.flatMap(cxnIO(_).run.transact(xa))
  // }

  // override def listKeys(did: PrismDID): Task[Seq[(String, ArraySeq[Byte], ECKeyPair)]] = {
  //   val cxnIO = sql"""
  //       | SELECT
  //       |   key_id,
  //       |   operation_hash,
  //       |   key_pair
  //       | FROM public.prism_did_secret_storage
  //       | WHERE
  //       |   did = $did
  //       """.stripMargin
  //     .query[(String, ArraySeq[Byte], ECKeyPair)]
  //     .to[List]

  //   cxnIO.transact(xa)
  // }

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
  val layer: URLayer[Transactor[Task] & Apollo, DIDSecretStorage] =
    ZLayer.fromFunction(new JdbcDIDSecretStorage(_, _))
}
