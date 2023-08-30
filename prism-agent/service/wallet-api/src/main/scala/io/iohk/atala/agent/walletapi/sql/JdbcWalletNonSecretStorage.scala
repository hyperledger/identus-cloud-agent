package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorage
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.db.Implicits.{*, given}
import io.iohk.atala.shared.models.WalletId
import java.time.Instant
import zio.*

class JdbcWalletNonSecretStorage(xa: Transactor[ContextAwareTask]) extends WalletNonSecretStorage {

  override def createWallet(wallet: Wallet): Task[Wallet] = {
    val cxnIO = (row: WalletRow) => sql"""
        | INSERT INTO public.wallet(
        |   wallet_id,
        |   name,
        |   created_at,
        |   updated_at
        | )
        | VALUES (
        |   ${row.id},
        |   ${row.name},
        |   ${row.createdAt},
        |   ${row.updatedAt}
        | )
        """.stripMargin.update

    val row = WalletRow.from(wallet)
    cxnIO(row).run
      .transactWithoutContext(xa)
      .as(wallet)
  }

  override def listWallet(offset: Option[Int], limit: Option[Int]): Task[(Seq[Wallet], Int)] = {
    val countCxnIO =
      sql"""
        | SELECT COUNT(*)
        | FROM public.wallet
        """.stripMargin
        .query[Int]
        .unique

    val baseFr =
      sql"""
        | SELECT
        |   wallet_id,
        |   name,
        |   created_at,
        |   updated_at
        | FROM public.wallet
        | ORDER BY created_at
        """.stripMargin
    val withOffsetFr = offset.fold(baseFr)(offsetValue => baseFr ++ fr"OFFSET $offsetValue")
    val withOffsetAndLimitFr = limit.fold(withOffsetFr)(limitValue => withOffsetFr ++ fr"LIMIT $limitValue")
    val walletsCxnIO = withOffsetAndLimitFr.query[WalletRow].to[List]

    val effect = for {
      totalCount <- countCxnIO
      rows <- walletsCxnIO.map(_.map(_.toDomain))
    } yield (rows, totalCount)

    effect.transactWithoutContext(xa)
  }

}

object JdbcWalletNonSecretStorage {
  val layer: URLayer[Transactor[ContextAwareTask], WalletNonSecretStorage] =
    ZLayer.fromFunction(new JdbcWalletNonSecretStorage(_))
}
