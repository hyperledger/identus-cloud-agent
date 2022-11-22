package io.iohk.atala.pollux.core.model.error

import java.util.UUID

sealed trait IssueCredentialError

object IssueCredentialError {
  final case class RepositoryError(cause: Throwable) extends IssueCredentialError
  final case class RecordIdNotFound(recordId: UUID) extends IssueCredentialError
  final case class ThreadIdNotFound(thid: UUID) extends IssueCredentialError
  final case class InvalidFlowStateError(msg: String) extends IssueCredentialError
  final case class UnexpectedError(msg: String) extends IssueCredentialError
}
