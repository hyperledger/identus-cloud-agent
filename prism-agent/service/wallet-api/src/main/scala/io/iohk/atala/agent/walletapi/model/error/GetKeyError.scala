package io.iohk.atala.agent.walletapi.model.error

sealed trait GetKeyError

object GetKeyError {
  final case class WalletStorageError(cause: Throwable) extends GetKeyError
  final case class KeyConstructionError(cause: Throwable) extends GetKeyError
}
