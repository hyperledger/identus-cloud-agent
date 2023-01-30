package io.iohk.atala.pollux.core.model.error

sealed trait CredentialRepositoryError extends Throwable

object CredentialRepositoryError {
  final case class UniqueConstraintViolation(message: String) extends CredentialRepositoryError
}
