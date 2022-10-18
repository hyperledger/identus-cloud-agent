package io.iohk.atala.agent.custodian.model.error

sealed trait CreateManagedDIDError

object CreateManagedDIDError {
  final case class KeyGenerationError(cause: Throwable) extends CreateManagedDIDError
}
