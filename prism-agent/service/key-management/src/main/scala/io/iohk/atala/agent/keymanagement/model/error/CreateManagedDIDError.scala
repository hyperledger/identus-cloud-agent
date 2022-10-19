package io.iohk.atala.agent.keymanagement.model.error

import io.iohk.atala.castor.core.model.error.DIDOperationError

sealed trait CreateManagedDIDError

object CreateManagedDIDError {
  final case class KeyGenerationError(cause: Throwable) extends CreateManagedDIDError
  final case class SecretStorageError(cause: Throwable) extends CreateManagedDIDError
  final case class OperationError(cause: DIDOperationError) extends CreateManagedDIDError
}
