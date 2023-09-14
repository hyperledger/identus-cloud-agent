package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorageRefinedError
import io.iohk.atala.event.notification.EventNotificationConfig
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

import java.util.UUID

sealed trait WalletManagementServiceError extends Throwable

object WalletManagementServiceError {
  final case class SeedGenerationError(cause: Throwable) extends WalletManagementServiceError {
    override def getCause(): Throwable = cause
  }
  final case class WalletStorageError(cause: Throwable) extends WalletManagementServiceError {
    override def getCause(): Throwable = cause
  }
  final case class TooManyWebhookError(limit: Int, actual: Int) extends WalletManagementServiceError {
    override def getMessage(): String = toString()
  }
  final case class DuplicatedWalletId(id: WalletId) extends WalletManagementServiceError {
    override def getMessage(): String = toString()
  }

  def fromStorageError(e: Throwable): WalletManagementServiceError = {
    e match {
      case re: WalletNonSecretStorageRefinedError =>
        re match {
          case WalletNonSecretStorageRefinedError.DuplicatedWalletId(id)        => DuplicatedWalletId(id)
          case WalletNonSecretStorageRefinedError.TooManyWebhook(limit, actual) => TooManyWebhookError(limit, actual)
        }
      case e => WalletStorageError(e)
    }
  }
}

trait WalletManagementService {
  def createWallet(wallet: Wallet, seed: Option[WalletSeed] = None): IO[WalletManagementServiceError, Wallet]

  def getWallet(walletId: WalletId): IO[WalletManagementServiceError, Option[Wallet]]

  /** @return A tuple containing a list of items and a count of total items */
  def listWallets(
      offset: Option[Int] = None,
      limit: Option[Int] = None
  ): IO[WalletManagementServiceError, (Seq[Wallet], Int)]

  def listWalletNotifications: ZIO[WalletAccessContext, WalletManagementServiceError, Seq[EventNotificationConfig]]

  def createWalletNotification(
      config: EventNotificationConfig
  ): ZIO[WalletAccessContext, WalletManagementServiceError, EventNotificationConfig]

  def deleteWalletNotification(id: UUID): ZIO[WalletAccessContext, WalletManagementServiceError, Unit]
}
