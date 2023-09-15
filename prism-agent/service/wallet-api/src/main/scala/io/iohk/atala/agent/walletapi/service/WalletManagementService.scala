package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorageError
import io.iohk.atala.event.notification.EventNotificationConfig
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

import java.util.UUID
import scala.language.implicitConversions

sealed trait WalletManagementServiceError {
  final def toThrowable: Throwable = this
}

object WalletManagementServiceError {
  final case class SeedGenerationError(cause: Throwable) extends WalletManagementServiceError
  final case class UnexpectedStorageError(cause: Throwable) extends WalletManagementServiceError
  final case class TooManyWebhookError(limit: Int, actual: Int) extends WalletManagementServiceError
  final case class DuplicatedWalletId(id: WalletId) extends WalletManagementServiceError
  final case class DuplicatedWalletSeed(id: WalletId) extends WalletManagementServiceError

  given Conversion[WalletNonSecretStorageError, WalletManagementServiceError] = {
    case WalletNonSecretStorageError.TooManyWebhook(limit, actual) => TooManyWebhookError(limit, actual)
    case WalletNonSecretStorageError.DuplicatedWalletId(id)        => DuplicatedWalletId(id)
    case WalletNonSecretStorageError.DuplicatedWalletSeed(id)      => DuplicatedWalletSeed(id)
    case WalletNonSecretStorageError.UnexpectedError(cause)        => UnexpectedStorageError(cause)
  }

  given Conversion[WalletManagementServiceError, Throwable] = {
    case SeedGenerationError(cause)    => Exception("Unable to generate wallet seed.", cause)
    case UnexpectedStorageError(cause) => Exception(cause)
    case TooManyWebhookError(limit, actual) =>
      Exception(s"Too many webhook created for a wallet. Limit $limit, Actual $actual.")
    case DuplicatedWalletId(id)   => Exception(s"Duplicated wallet id: $id")
    case DuplicatedWalletSeed(id) => Exception(s"Duplicated wallet seed for wallet id: $id")
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
