package io.iohk.atala.pollux.core.model.error

sealed trait IssueCredentialError

object IssueCredentialError {
  final case class RepositoryError(cause: Throwable) extends IssueCredentialError
}
