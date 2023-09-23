package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.event.notification.EventNotificationConfig
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import org.postgresql.util.PSQLException
import zio.*

import java.util.UUID

sealed trait WalletNonSecretStorageError

object WalletNonSecretStorageError {
  final case class DuplicatedWalletId(id: WalletId) extends WalletNonSecretStorageError
  final case class DuplicatedWalletSeed(id: WalletId) extends WalletNonSecretStorageError
  final case class UnexpectedError(cause: Throwable) extends WalletNonSecretStorageError
  final case class TooManyWebhook(limit: Int, actual: Int)
      extends Throwable("Too many webhook is created for a wallet"),
        WalletNonSecretStorageError

  def fromWalletOps(walletId: WalletId)(e: Throwable): WalletNonSecretStorageError = {
    e match {
      /* PSQLState.UNIQUE_VIOLATION */
      case e: PSQLException if e.getSQLState() == "23505" && e.getMessage().contains("wallet_id") =>
        DuplicatedWalletId(walletId)
      /* PSQLState.UNIQUE_VIOLATION */
      case e: PSQLException if e.getSQLState() == "23505" && e.getMessage().contains("wallet_seed_digest") =>
        DuplicatedWalletSeed(walletId)
      case e => UnexpectedError(e)
    }
  }

}

trait WalletNonSecretStorage {
  def createWallet(wallet: Wallet, seedDigest: Array[Byte]): IO[WalletNonSecretStorageError, Wallet]
  def getWallet(walletId: WalletId): IO[WalletNonSecretStorageError, Option[Wallet]]
  def listWallet(
      offset: Option[Int] = None,
      limit: Option[Int] = None
  ): IO[WalletNonSecretStorageError, (Seq[Wallet], Int)]
  def createWalletNotification(
      config: EventNotificationConfig
  ): ZIO[WalletAccessContext, WalletNonSecretStorageError, EventNotificationConfig]
  def walletNotification: ZIO[WalletAccessContext, WalletNonSecretStorageError, Seq[EventNotificationConfig]]
  def deleteWalletNotification(id: UUID): ZIO[WalletAccessContext, WalletNonSecretStorageError, Unit]
}
