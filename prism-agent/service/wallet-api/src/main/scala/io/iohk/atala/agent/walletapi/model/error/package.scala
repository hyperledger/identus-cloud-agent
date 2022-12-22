package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.error.DIDOperationError

package object error {
  final case class CommonWalletStorageError(cause: Throwable)

  given Conversion[CommonWalletStorageError, PublishManagedDIDError] = e =>
    PublishManagedDIDError.WalletStorageError(e.cause)
  given Conversion[DIDOperationError, PublishManagedDIDError] = PublishManagedDIDError.OperationError(_)

  given Conversion[CommonWalletStorageError, GetManagedDIDError] = e => GetManagedDIDError.WalletStorageError(e.cause)
  given Conversion[DIDOperationError, GetManagedDIDError] = GetManagedDIDError.OperationError(_)
}
