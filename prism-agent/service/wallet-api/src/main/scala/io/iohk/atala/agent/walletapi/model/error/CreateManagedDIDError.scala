package io.iohk.atala.agent.walletapi.model.error

import io.iohk.atala.castor.core.model.error as castor

sealed trait CreateManagedDIDError extends Throwable

object CreateManagedDIDError {
  final case class InvalidArgument(msg: String) extends CreateManagedDIDError
  final case class KeyGenerationError(cause: Throwable) extends CreateManagedDIDError
  final case class WalletStorageError(cause: Throwable) extends CreateManagedDIDError
  final case class InvalidOperation(cause: castor.OperationValidationError) extends CreateManagedDIDError
}
