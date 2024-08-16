package org.hyperledger.identus.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import org.hyperledger.identus.agent.walletapi.model.Wallet
import org.hyperledger.identus.agent.walletapi.sql.model.WalletSql
import org.hyperledger.identus.agent.walletapi.sql.model as quillModel
import org.hyperledger.identus.agent.walletapi.storage.WalletNonSecretStorage
import org.hyperledger.identus.event.notification.EventNotificationConfig
import org.hyperledger.identus.shared.db.ContextAwareTask
import org.hyperledger.identus.shared.db.Implicits.{*, given}
import org.hyperledger.identus.shared.models.{WalletAccessContext, WalletId}
import quillModel.WalletNotificationSql
import zio.*

import java.net.URL
import java.time.Instant
import java.util.UUID

class JdbcWalletNonSecretStorage(xa: Transactor[ContextAwareTask]) extends WalletNonSecretStorage {

  override def createWallet(wallet: Wallet, seedDigest: Array[Byte]): UIO[Wallet] = {
    WalletSql
      .insert(quillModel.Wallet.from(wallet, seedDigest))
      .transactWithoutContext(xa)
      .orDie
      .map(_.toModel)
  }

  override def findWalletById(walletId: WalletId): UIO[Option[Wallet]] = {
    WalletSql
      .findByIds(Seq(walletId))
      .transactWithoutContext(xa)
      .orDie
      .map(_.headOption.map(_.toModel))
  }

  override def findWalletBySeed(seedDigest: Array[Byte]): UIO[Option[Wallet]] = {
    WalletSql
      .findBySeed(seedDigest)
      .transactWithoutContext(xa)
      .orDie
      .map(_.headOption.map(_.toModel))
  }

  override def getWallets(walletIds: Seq[WalletId]): UIO[Seq[Wallet]] = {
    walletIds match
      case Nil => ZIO.succeed(Nil)
      case ids =>
        WalletSql
          .findByIds(ids)
          .transactWithoutContext(xa)
          .orDie
          .map(_.map(_.toModel))
  }

  override def listWallet(
      offset: Option[Int],
      limit: Option[Int]
  ): UIO[(Seq[Wallet], RuntimeFlags)] = {
    val countCxnIO = WalletSql.lookupCount()

    val walletsCxnIO = WalletSql.lookup(
      offset = offset.getOrElse(0),
      limit = limit.getOrElse(1000)
    )

    val effect = for {
      totalCount <- countCxnIO
      rows <- walletsCxnIO.map(_.map(_.toModel))
    } yield (rows, totalCount.toInt)

    effect
      .transactWithoutContext(xa)
      .orDie
  }

  override def countWalletNotification: URIO[WalletAccessContext, Int] = {
    val countIO = sql"""
        | SELECT COUNT(*)
        | FROM public.wallet_notification
        """.stripMargin
      .query[Int]
      .unique

    countIO
      .transactWallet(xa)
      .orDie
  }

  override def createWalletNotification(
      config: EventNotificationConfig
  ): URIO[WalletAccessContext, Unit] = {
    WalletNotificationSql
      .insert(quillModel.WalletNotification.from(config))
      .transactWallet(xa)
      .ensureOneAffectedRowOrDie
  }

  override def walletNotification: URIO[WalletAccessContext, Seq[EventNotificationConfig]] = {
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
      .orDie
  }

  override def deleteWalletNotification(id: UUID): URIO[WalletAccessContext, Unit] = {
    val cxn =
      sql"""
        | DELETE FROM public.wallet_notification
        | WHERE id = $id
        """.stripMargin.update

    cxn.run
      .transactWallet(xa)
      .ensureOneAffectedRowOrDie
  }

}

object JdbcWalletNonSecretStorage {

  val layer: URLayer[Transactor[ContextAwareTask], WalletNonSecretStorage] =
    ZLayer.fromFunction(new JdbcWalletNonSecretStorage(_))

}
