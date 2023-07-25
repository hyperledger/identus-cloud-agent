package io.iohk.atala.agent.walletapi.sql

import com.nimbusds.jose.jwk.OctetKeyPair
import doobie.*
import doobie.implicits.*
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
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

  override def getKey(did: DidId, keyId: String): RIO[WalletAccessContext, Option[OctetKeyPair]] = {
    val cxnIO = (walletId: WalletId) =>
      sql"""
        | SELECT
        |   key_pair
        | FROM public.peer_did_rand_key
        | WHERE
        |   did = $did
        |   AND key_id = $keyId
        |   AND wallet_id = $walletId
        """.stripMargin
        .query[OctetKeyPair]
        .option

    ZIO.serviceWithZIO[WalletAccessContext](ctx => cxnIO(ctx.walletId).transact(xa))
  }

  override def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): RIO[WalletAccessContext, Int] = {
    val cxnIO = (now: InstantAsBigInt, walletId: WalletId) => sql"""
        | INSERT INTO public.peer_did_rand_key(
        |   did,
        |   created_at,
        |   key_id,
        |   key_pair,
        |   wallet_id
        | ) values (
        |   ${did},
        |   ${now},
        |   ${keyId},
        |   ${keyPair},
        |   ${walletId}
        | )
        """.stripMargin.update

    for {
      now <- Clock.instant
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      n <- cxnIO(InstantAsBigInt(now), walletId).run.transact(xa)
    } yield n
  }

}

object JdbcDIDSecretStorage {
  val layer: URLayer[Transactor[Task], DIDSecretStorage] =
    ZLayer.fromFunction(new JdbcDIDSecretStorage(_))
}
