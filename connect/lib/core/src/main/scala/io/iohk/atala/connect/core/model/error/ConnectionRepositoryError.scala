package io.iohk.atala.connect.core.model.error

sealed trait ConnectionRepositoryError extends Throwable

object ConnectionRepositoryError {
  final case class UniqueConstraintViolation(message: String) extends ConnectionRepositoryError
}
