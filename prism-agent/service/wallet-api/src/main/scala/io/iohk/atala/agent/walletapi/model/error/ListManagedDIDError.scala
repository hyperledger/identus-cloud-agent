package io.iohk.atala.agent.walletapi.model.error

import io.iohk.atala.castor.core.model.error.DIDOperationError

sealed trait ListManagedDIDError

object ListManagedDIDError {
  final case class WalletStorageError(cause: Throwable) extends ListManagedDIDError
  final case class OperationError(cause: DIDOperationError) extends ListManagedDIDError
}
