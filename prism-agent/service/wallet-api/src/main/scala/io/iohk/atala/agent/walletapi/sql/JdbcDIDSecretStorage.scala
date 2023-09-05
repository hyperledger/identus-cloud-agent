package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import io.iohk.atala.agent.walletapi.storage.DIDSecret
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.mercury.model.DidId
import zio.*
import zio.interop.catz.*
import zio.json.*
import zio.json.ast.Json
import zio.json.ast.Json.*

import java.time.Instant
import java.util.UUID

class JdbcDIDSecretStorage(xa: Transactor[Task]) extends DIDSecretStorage {

  case class InstantAsBigInt(value: Instant)

  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString)

  given uuidPut: Put[UUID] = Put[String].contramap(_.toString())

  given instantGet: Get[InstantAsBigInt] = Get[Long].map(Instant.ofEpochSecond).map(InstantAsBigInt.apply)

  given instantPut: Put[InstantAsBigInt] = Put[Long].contramap(_.value.getEpochSecond())

  given didIdGet: Get[DidId] = Get[String].map(DidId(_))

  given didIdPut: Put[DidId] = Put[String].contramap(_.value)

  given didSecretGet: Read[DIDSecret] =
    Read[(Json, String)].map { case (json, schemaId) => DIDSecret(json, schemaId) }

  given didSecretPut: Write[DIDSecret] =
    Write[(Json, String)].contramap(ds => (ds.json, ds.schemaId))

  given jsonGet: Get[Json] = Get[String].map(_.fromJson[Json] match {
    case Right(value) => value
    case Left(error)  => throw new RuntimeException(error)
  })

  given jsonPut: Put[Json] = Put[String].contramap(_.toString())

  override def getKey(did: DidId, keyId: String, schemaId: String): Task[Option[DIDSecret]] = {
    val cxnIO = sql"""
                     | SELECT
                     |   key_pair,
                     |   schema_id
                     | FROM public.did_secret_storage
                     | WHERE
                     |   did = $did
                     |   AND schema_id = $schemaId
                     |   AND key_id = $keyId
        """.stripMargin
      .query[DIDSecret]
      .option

    cxnIO.transact(xa)
  }

  override def insertKey(did: DidId, keyId: String, didSecret: DIDSecret): Task[Int] = {
    val cxnIO = (now: InstantAsBigInt) => sql"""
           | INSERT INTO public.did_secret_storage(
           |   did,
           |   created_at,
           |   key_id,
           |   key_pair,
           |   schema_id
           | ) values (
           |   ${did},
           |   ${now},
           |   ${keyId},
           |   ${didSecret.json},
           |   ${didSecret.schemaId}
           | )
        """.stripMargin.update

    Clock.instant.flatMap(i => cxnIO(InstantAsBigInt(i)).run.transact(xa))
  }

}

object JdbcDIDSecretStorage {
  val layer: URLayer[Transactor[Task], DIDSecretStorage] =
    ZLayer.fromFunction(new JdbcDIDSecretStorage(_))
}
