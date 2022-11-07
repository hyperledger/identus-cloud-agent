package io.iohk.atala.agent.walletapi.model.error

import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.error.DIDOperationError

sealed trait PublishManagedDIDError

object PublishManagedDIDError {
  final case class DIDNotFound(did: PrismDID) extends PublishManagedDIDError
  final case class WalletStorageError(cause: Throwable) extends PublishManagedDIDError
  final case class OperationError(cause: DIDOperationError) extends PublishManagedDIDError
}
