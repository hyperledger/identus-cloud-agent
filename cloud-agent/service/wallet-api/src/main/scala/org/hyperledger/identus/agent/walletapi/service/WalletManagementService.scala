package org.hyperledger.identus.agent.walletapi.service

import org.hyperledger.identus.agent.walletapi.model.Wallet
import org.hyperledger.identus.agent.walletapi.model.WalletSeed
import org.hyperledger.identus.agent.walletapi.storage.WalletNonSecretStorageError
import org.hyperledger.identus.event.notification.EventNotificationConfig
import org.hyperledger.identus.shared.models.WalletAccessContext
import org.hyperledger.identus.shared.models.WalletId
import zio.*

import java.util.UUID
import scala.language.implicitConversions
import org.hyperledger.identus.shared.models.WalletAdministrationContext

sealed trait WalletManagementServiceError {
  final def toThrowable: Throwable = this
}

object WalletManagementServiceError {
  final case class UnexpectedStorageError(cause: Throwable) extends WalletManagementServiceError
  final case class TooManyWebhookError(limit: Int, actual: Int) extends WalletManagementServiceError
  final case class DuplicatedWalletId(id: WalletId) extends WalletManagementServiceError
  final case class DuplicatedWalletSeed(id: WalletId) extends WalletManagementServiceError
  final case class TooManyPermittedWallet() extends WalletManagementServiceError

  given Conversion[WalletNonSecretStorageError, WalletManagementServiceError] = {
    case WalletNonSecretStorageError.TooManyWebhook(limit, actual) => TooManyWebhookError(limit, actual)
    case WalletNonSecretStorageError.DuplicatedWalletId(id)        => DuplicatedWalletId(id)
    case WalletNonSecretStorageError.DuplicatedWalletSeed(id)      => DuplicatedWalletSeed(id)
    case WalletNonSecretStorageError.UnexpectedError(cause)        => UnexpectedStorageError(cause)
  }

  given Conversion[WalletManagementServiceError, Throwable] = {
    case UnexpectedStorageError(cause) => Exception(cause)
    case TooManyWebhookError(limit, actual) =>
      Exception(s"Too many webhook created for a wallet. Limit $limit, Actual $actual.")
    case DuplicatedWalletId(id)   => Exception(s"Duplicated wallet id: $id")
    case DuplicatedWalletSeed(id) => Exception(s"Duplicated wallet seed for wallet id: $id")
    case TooManyPermittedWallet() =>
      Exception(s"The operation is not allowed because wallet access already exists for the current user.")
  }

}

trait WalletManagementService {
  def createWallet(
      wallet: Wallet,
      seed: Option[WalletSeed] = None
  ): ZIO[WalletAdministrationContext, WalletManagementServiceError, Wallet]

  def getWallet(walletId: WalletId): ZIO[WalletAdministrationContext, WalletManagementServiceError, Option[Wallet]]

  def getWallets(walletIds: Seq[WalletId]): ZIO[WalletAdministrationContext, WalletManagementServiceError, Seq[Wallet]]

  /** @return A tuple containing a list of items and a count of total items */
  def listWallets(
      offset: Option[Int] = None,
      limit: Option[Int] = None
  ): ZIO[WalletAdministrationContext, WalletManagementServiceError, (Seq[Wallet], Int)]

  def listWalletNotifications: ZIO[WalletAccessContext, WalletManagementServiceError, Seq[EventNotificationConfig]]

  def createWalletNotification(
      config: EventNotificationConfig
  ): ZIO[WalletAccessContext, WalletManagementServiceError, EventNotificationConfig]

  def deleteWalletNotification(id: UUID): ZIO[WalletAccessContext, WalletManagementServiceError, Unit]
}
