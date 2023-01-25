package io.iohk.atala.agent.walletapi.model.error

import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.error.DIDOperationError

sealed trait CreateManagedDIDError extends Throwable

object CreateManagedDIDError {
  final case class InvalidArgument(msg: String) extends CreateManagedDIDError
  final case class DIDAlreadyExists(did: PrismDID) extends CreateManagedDIDError
  final case class KeyGenerationError(cause: Throwable) extends CreateManagedDIDError
  final case class WalletStorageError(cause: Throwable) extends CreateManagedDIDError
  final case class OperationError(cause: DIDOperationError) extends CreateManagedDIDError
}
