package io.iohk.atala.agent.walletapi.sql

import com.nimbusds.jose.jwk.JWK
import doobie.*
import doobie.implicits.*
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.mercury.model.DidId
import zio.*
import zio.interop.catz.*

import java.time.Instant
import java.util.UUID

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
  given jwkGet: Get[JWK] = Get[String].map(JWK.parse)
  given jwkPut: Put[JWK] = Put[String].contramap(_.toJSONString)

  override def getKey(did: DidId, keyId: String): Task[Option[JWK]] = {
    val cxnIO = sql"""
        | SELECT
        |   key_pair
        | FROM public.did_secret_storage
        | WHERE
        |   did = $did
        |   AND key_id = $keyId
        """.stripMargin
      .query[JWK]
      .option

    cxnIO.transact(xa)
  }

  override def insertKey(did: DidId, keyId: String, keyPair: JWK): Task[Int] = {
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
