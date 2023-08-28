package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import io.iohk.atala.agent.walletapi.storage.DIDSecret
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.db.Implicits.{*, given}
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.json.ast.Json.*

import java.time.Instant
import java.util.UUID
import io.iohk.atala.shared.models.WalletId

class JdbcDIDSecretStorage(xa: Transactor[ContextAwareTask]) extends DIDSecretStorage {

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

  override def getKey(did: DidId, keyId: String, schemaId: String): RIO[WalletAccessContext, Option[DIDSecret]] = {
    val cxnIO = sql"""
         | SELECT
         |   key_pair,
         |   schema_id
         | FROM public.peer_did_rand_key
         | WHERE
         |   did = $did
         |   AND schema_id = $schemaId
         |   AND key_id = $keyId
        """.stripMargin
      .query[DIDSecret]
      .option

    cxnIO.transactWallet(xa)
  }

  override def insertKey(did: DidId, keyId: String, didSecret: DIDSecret): RIO[WalletAccessContext, Int] = {
    val cxnIO = (now: InstantAsBigInt, walletId: WalletId) => sql"""
        | INSERT INTO public.peer_did_rand_key(
        |   did,
        |   created_at,
        |   key_id,
        |   key_pair,
        |   schema_id,
        |   wallet_id
        | ) values (
        |   ${did},
        |   ${now},
        |   ${keyId},
        |   ${didSecret.json},
        |   ${didSecret.schemaId},
        |   ${walletId}
        | )
        """.stripMargin.update

    for {
      now <- Clock.instant
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      result <- cxnIO(InstantAsBigInt(now), walletId).run.transactWallet(xa)
    } yield result
  }

}

object JdbcDIDSecretStorage {
  val layer: URLayer[Transactor[ContextAwareTask], DIDSecretStorage] =
    ZLayer.fromFunction(new JdbcDIDSecretStorage(_))
}
