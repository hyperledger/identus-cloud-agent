package org.hyperledger.identus.agent.walletapi.model.error

import org.hyperledger.identus.castor.core.model.error.DIDOperationError

sealed trait GetManagedDIDError

object GetManagedDIDError {
  final case class WalletStorageError(cause: Throwable) extends GetManagedDIDError
  final case class OperationError(cause: DIDOperationError) extends GetManagedDIDError
}
