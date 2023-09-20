package io.iohk.atala.agent.walletapi.sql

import com.nimbusds.jose.jwk.OctetKeyPair
import doobie.*
import doobie.implicits.*
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.db.Implicits.*
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.time.Instant
import java.util.UUID

class JdbcDIDSecretStorage(xa: Transactor[ContextAwareTask]) extends DIDSecretStorage {

  case class InstantAsBigInt(value: Instant)

  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString)

  given uuidPut: Put[UUID] = Put[String].contramap(_.toString())

  given instantGet: Get[InstantAsBigInt] = Get[Long].map(Instant.ofEpochSecond).map(InstantAsBigInt.apply)

  given instantPut: Put[InstantAsBigInt] = Put[Long].contramap(_.value.getEpochSecond())

  given didIdGet: Get[DidId] = Get[String].map(DidId(_))

  given didIdPut: Put[DidId] = Put[String].contramap(_.value)

  override def getKey(did: DidId, keyId: String): RIO[WalletAccessContext, Option[OctetKeyPair]] = {
    val cxnIO = sql"""
         | SELECT key_pair
         | FROM public.peer_did_rand_key
         | WHERE
         |   did = $did
         |   AND key_id = $keyId
        """.stripMargin
      .query[OctetKeyPair]
      .option

    cxnIO.transactWallet(xa)
  }

  override def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): RIO[WalletAccessContext, Int] = {
    val cxnIO = (now: InstantAsBigInt) => sql"""
        | INSERT INTO public.peer_did_rand_key(
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

    for {
      now <- Clock.instant
      result <- cxnIO(InstantAsBigInt(now)).run.transactWallet(xa)
    } yield result
  }

}

object JdbcDIDSecretStorage {
  val layer: URLayer[Transactor[ContextAwareTask], DIDSecretStorage] =
    ZLayer.fromFunction(new JdbcDIDSecretStorage(_))
}
