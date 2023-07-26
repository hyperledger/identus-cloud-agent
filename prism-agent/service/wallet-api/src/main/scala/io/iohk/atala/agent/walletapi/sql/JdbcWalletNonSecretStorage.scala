package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorage
import io.iohk.atala.shared.models.WalletId
import java.time.Instant
import zio.*
import zio.interop.catz.*

class JdbcWalletNonSecretStorage(xa: Transactor[Task]) extends WalletNonSecretStorage {

  override def createWallet: Task[WalletId] = {
    val cxnIO = (now: Instant) =>
      sql"""
        | INSERT INTO public.wallet(created_at)
        | VALUES ($now)
        """.stripMargin.update
        .withUniqueGeneratedKeys[WalletId]("wallet_id")

    for {
      now <- Clock.instant
      walletId <- cxnIO(now).transact(xa)
    } yield walletId
  }

  override def listWallet: Task[Seq[WalletId]] = {
    val cxnIO =
      sql"""
           | SELECT wallet_id
           | FROM public.wallet
           """.stripMargin
        .query[WalletId]
        .to[List]

    cxnIO.transact(xa)
  }

}

object JdbcWalletNonSecretStorage {
  val layer: URLayer[Transactor[Task], WalletNonSecretStorage] =
    ZLayer.fromFunction(new JdbcWalletNonSecretStorage(_))
}
