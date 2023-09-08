package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.event.notification.EventNotificationConfig
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

import java.util.UUID

object WalletNonSecretStorageCustomError {
  final case class TooManyWebhook(limit: Int, actual: Int) extends Throwable
}

trait WalletNonSecretStorage {
  def createWallet(wallet: Wallet): Task[Wallet]
  def getWallet(walletId: WalletId): Task[Option[Wallet]]
  def listWallet(offset: Option[Int] = None, limit: Option[Int] = None): Task[(Seq[Wallet], Int)]
  def createWalletNotification(config: EventNotificationConfig): RIO[WalletAccessContext, EventNotificationConfig]
  def walletNotification: RIO[WalletAccessContext, Seq[EventNotificationConfig]]
  def deleteWalletNotification(id: UUID): RIO[WalletAccessContext, Unit]
}
