package io.iohk.atala.pollux.core.model

sealed trait CredentialError

object CredentialError {
  final case class RepositoryError(cause: Throwable) extends CredentialError
}
