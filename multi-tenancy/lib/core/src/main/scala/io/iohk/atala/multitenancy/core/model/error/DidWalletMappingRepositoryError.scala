package io.iohk.atala.multitenancy.core.model.error

sealed trait DidWalletMappingRepositoryError extends Throwable

object DidWalletMappingRepositoryError {
  final case class UniqueConstraintViolation(message: String) extends DidWalletMappingRepositoryError
}
