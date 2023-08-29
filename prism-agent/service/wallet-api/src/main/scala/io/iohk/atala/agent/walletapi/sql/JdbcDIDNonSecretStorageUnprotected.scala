package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.iohk.atala.agent.walletapi.model.*
import io.iohk.atala.agent.walletapi.storage.DIDNonSecretStorageUnprotected
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.models.WalletId
import zio.*
import zio.interop.catz.*

import java.time.Instant
import java.util.UUID

class JdbcDIDNonSecretStorageUnprotected(xa: Transactor[Task]) extends DIDNonSecretStorageUnprotected {

  given walletIdGet: Get[WalletId] = Get[UUID].map(WalletId.fromUUID)
  given walletIdPut: Put[WalletId] = Put[UUID].contramap(_.toUUID)

  override def getPeerDIDRecord(did: DidId): Task[Option[PeerDIDRecord]] = {
    val cnxIO =
      sql"""
           | SELECT
           |  did,
           |  created_at,
           |  wallet_id
           | FROM public.peer_did
           | WHERE
           |  did = $did
            """.stripMargin
        .query[PeerDIDRecord]
        .option

    cnxIO.transact(xa)
  }

}

object JdbcDIDNonSecretStorageUnprotected {
  val layer: URLayer[Transactor[Task], DIDNonSecretStorageUnprotected] =
    ZLayer.fromFunction(new JdbcDIDNonSecretStorageUnprotected(_))
}
