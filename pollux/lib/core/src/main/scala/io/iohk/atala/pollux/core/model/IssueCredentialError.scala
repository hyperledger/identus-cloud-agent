package io.iohk.atala.pollux.core.model

sealed trait IssueCredentialError

object IssueCredentialError {
  final case class RepositoryError(cause: Throwable) extends IssueCredentialError
}
