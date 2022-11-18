package io.iohk.atala.connect.core.model.error

sealed trait ConnectionError

object ConnectionError {
  final case class RepositoryError(cause: Throwable) extends ConnectionError
}
