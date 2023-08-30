package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.model.WalletSeed
import zio.*

sealed trait WalletManagementServiceError extends Throwable
object WalletManagementServiceError {
  final case class SeedGenerationError(cause: Throwable) extends WalletManagementServiceError
  final case class WalletStorageError(cause: Throwable) extends WalletManagementServiceError
}

trait WalletManagementService {
  def createWallet(wallet: Wallet, seed: Option[WalletSeed] = None): IO[WalletManagementServiceError, Wallet]

  /** @return A tuple containing a list of items and a count of total items */
  def listWallets(
      offset: Option[Int] = None,
      limit: Option[Int] = None
  ): IO[WalletManagementServiceError, (Seq[Wallet], Int)]
}
