package io.iohk.atala.agent.walletapi.sql

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorageCustomError.TooManyWebhook
import io.iohk.atala.event.notification.EventNotificationConfig
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.db.Implicits.{*, given}
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

import java.net.URL
import java.time.Instant
import java.util.UUID

class JdbcWalletNonSecretStorage(xa: Transactor[ContextAwareTask]) extends WalletNonSecretStorage {

  private val MAX_WEBHOOK_PER_WALLET = 1

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

  override def getWallet(walletId: WalletId): Task[Option[Wallet]] = {
    val cxnIO =
      sql"""
        | SELECT
        |   wallet_id,
        |   name,
        |   created_at,
        |   updated_at
        | FROM public.wallet
        | WHERE wallet_id = $walletId
        """.stripMargin
        .query[WalletRow]
        .option

    cxnIO.transactWithoutContext(xa).map(_.map(_.toDomain))
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

  override def createWalletNotification(
      config: EventNotificationConfig
  ): RIO[WalletAccessContext, EventNotificationConfig] = {
    val cxn = (row: WalletNofiticationRow) => sql"""
        | INSERT INTO public.wallet_notification (
        |   id,
        |   wallet_id,
        |   url,
        |   custom_headers,
        |   created_at
        | ) VALUES (
        |   ${row.id},
        |   ${row.walletId},
        |   ${row.url},
        |   ${row.customHeaders},
        |   ${row.createdAt}
        | )
        """.stripMargin.update

    val countCxn = sql"""
        | SELECT COUNT(*)
        | FROM public.wallet_notification
        """.stripMargin
      .query[Int]
      .unique

    val row = WalletNofiticationRow.from(config)
    val effect = for {
      _ <- cxn(row).run
      count <- countCxn
      _ <-
        if (count <= MAX_WEBHOOK_PER_WALLET) ().pure[ConnectionIO]
        else TooManyWebhook(limit = MAX_WEBHOOK_PER_WALLET, actual = count).raiseError[ConnectionIO, Unit]
    } yield config

    effect.transactWallet(xa)
  }

  override def walletNotification: RIO[WalletAccessContext, Seq[EventNotificationConfig]] = {
    val cxn =
      sql"""
        | SELECT
        |   id,
        |   wallet_id,
        |   url,
        |   custom_headers,
        |   created_at
        | FROM public.wallet_notification
        """.stripMargin
        .query[WalletNofiticationRow]
        .to[List]

    cxn
      .transactWallet(xa)
      .flatMap(rows => ZIO.foreach(rows) { row => ZIO.fromTry(row.toDomain) })
  }

  override def deleteWalletNotification(id: UUID): RIO[WalletAccessContext, Unit] = {
    val cxn =
      sql"""
        | DELETE FROM public.wallet_notification
        | WHERE id = ${id}
        """.stripMargin.update

    cxn.run.transactWallet(xa).unit
  }

}

object JdbcWalletNonSecretStorage {
  val layer: URLayer[Transactor[ContextAwareTask], WalletNonSecretStorage] =
    ZLayer.fromFunction(new JdbcWalletNonSecretStorage(_))
}
