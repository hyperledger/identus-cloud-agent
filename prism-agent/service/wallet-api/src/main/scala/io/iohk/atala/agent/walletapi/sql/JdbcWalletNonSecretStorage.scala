package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorage
import io.iohk.atala.shared.db.Implicits.{*, given}
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.models.WalletId
import java.time.Instant
import zio.*

class JdbcWalletNonSecretStorage(xa: Transactor[ContextAwareTask]) extends WalletNonSecretStorage {

  override def createWallet: Task[WalletId] = {
    val cxnIO = (walletId: WalletId, now: Instant) => sql"""
        | INSERT INTO public.wallet(wallet_id, created_at)
        | VALUES ($walletId, $now)
        """.stripMargin.update

    for {
      now <- Clock.instant
      walletId = WalletId.random
      _ <- cxnIO(walletId, now).run.transactWithoutContext(xa)
    } yield walletId
  }

  override def listWallet(offset: Option[Int], limit: Option[Int]): Task[(Seq[WalletId], Int)] = {
    val countCxnIO =
      sql"""
        | SELECT COUNT(*)
        | FROM public.wallet
        """.stripMargin
        .query[Int]
        .unique

    val baseFr =
      sql"""
        | SELECT wallet_id
        | FROM public.wallet
        | ORDER BY created_at
        """.stripMargin
    val withOffsetFr = offset.fold(baseFr)(offsetValue => baseFr ++ fr"OFFSET $offsetValue")
    val withOffsetAndLimitFr = limit.fold(withOffsetFr)(limitValue => withOffsetFr ++ fr"LIMIT $limitValue")
    val walletsCxnIO = withOffsetAndLimitFr.query[WalletId].to[List]

    val effect = for {
      totalCount <- countCxnIO
      rows <- walletsCxnIO
    } yield (rows, totalCount)

    effect.transactWithoutContext(xa)
  }

}

object JdbcWalletNonSecretStorage {
  val layer: URLayer[Transactor[ContextAwareTask], WalletNonSecretStorage] =
    ZLayer.fromFunction(new JdbcWalletNonSecretStorage(_))
}
