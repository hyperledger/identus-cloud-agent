package io.iohk.atala.agent.walletapi.model.error

import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.castor.core.model.error.DIDOperationError

sealed trait UpdateManagedDIDError

object UpdateManagedDIDError {
  final case class DIDNotFound(did: CanonicalPrismDID) extends UpdateManagedDIDError
  final case class DIDNotPublished(did: CanonicalPrismDID) extends UpdateManagedDIDError
  final case class InvalidArgument(msg: String) extends UpdateManagedDIDError
  final case class KeyGenerationError(cause: Throwable) extends UpdateManagedDIDError
  final case class WalletStorageError(cause: Throwable) extends UpdateManagedDIDError
  final case class OperationError(cause: DIDOperationError) extends UpdateManagedDIDError
  final case class CryptographyError(cause: Throwable) extends UpdateManagedDIDError
}
