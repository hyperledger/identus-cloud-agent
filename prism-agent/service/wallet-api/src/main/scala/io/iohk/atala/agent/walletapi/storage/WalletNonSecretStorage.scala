package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.event.notification.EventNotificationConfig
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import org.postgresql.util.PSQLException
import zio.*

import java.util.UUID

sealed trait WalletNonSecretStorageRefinedError extends Throwable

object WalletNonSecretStorageRefinedError {
  final case class TooManyWebhook(limit: Int, actual: Int) extends WalletNonSecretStorageRefinedError {
    override def getMessage(): String = toString()
  }

  final case class DuplicatedWalletId(id: WalletId) extends WalletNonSecretStorageRefinedError {
    override def getMessage(): String = toString()
  }

  def refineWith(walletId: WalletId): PartialFunction[Throwable, WalletNonSecretStorageRefinedError] = {
    case e: PSQLException if e.getSQLState() == "23505" /* PSQLState.UNIQUE_VIOLATION */ => DuplicatedWalletId(walletId)
  }
}

trait WalletNonSecretStorage {
  def createWallet(wallet: Wallet): Task[Wallet]
  def getWallet(walletId: WalletId): Task[Option[Wallet]]
  def listWallet(offset: Option[Int] = None, limit: Option[Int] = None): Task[(Seq[Wallet], Int)]
  def createWalletNotification(config: EventNotificationConfig): RIO[WalletAccessContext, EventNotificationConfig]
  def walletNotification: RIO[WalletAccessContext, Seq[EventNotificationConfig]]
  def deleteWalletNotification(id: UUID): RIO[WalletAccessContext, Unit]
}
