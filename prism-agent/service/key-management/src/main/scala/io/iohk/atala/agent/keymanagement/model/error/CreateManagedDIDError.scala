package io.iohk.atala.agent.keymanagement.model.error

sealed trait CreateManagedDIDError

object CreateManagedDIDError {
  final case class KeyGenerationError(cause: Throwable) extends CreateManagedDIDError
  final case class SecretStorageError(cause: Throwable) extends CreateManagedDIDError
}
