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

class JdbcDIDSecretStorage(xa: Transactor[Task]) extends DIDSecretStorage {

  case class InstantAsBigInt(value: Instant)

  // Uncomment to have Doobie LogHandler in scope and automatically output SQL statements in logs
  // given logHandler: LogHandler = LogHandler.jdkLogHandler

  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  given uuidPut: Put[UUID] = Put[String].contramap(_.toString())

  given instantGet: Get[InstantAsBigInt] = Get[Long].map(Instant.ofEpochSecond).map(InstantAsBigInt.apply)
  given instantPut: Put[InstantAsBigInt] = Put[Long].contramap(_.value.getEpochSecond())

  given didIdGet: Get[DidId] = Get[String].map(DidId(_))
  given didIdPut: Put[DidId] = Put[String].contramap(_.value)
  given octetKeyPairGet: Get[OctetKeyPair] = Get[String].map(OctetKeyPair.parse)
  given octetKeyPairPut: Put[OctetKeyPair] = Put[String].contramap(_.toJSONString)

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
