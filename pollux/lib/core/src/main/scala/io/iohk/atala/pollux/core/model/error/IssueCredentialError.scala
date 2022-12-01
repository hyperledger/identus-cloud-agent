package io.iohk.atala.pollux.core.model.error

import java.util.UUID
import io.iohk.atala.pollux.vc.jwt.W3cCredentialPayload

sealed trait IssueCredentialError

object IssueCredentialError {
  final case class RepositoryError(cause: Throwable) extends IssueCredentialError
  final case class RecordIdNotFound(recordId: UUID) extends IssueCredentialError
  final case class ThreadIdNotFound(thid: UUID) extends IssueCredentialError
  final case class InvalidFlowStateError(msg: String) extends IssueCredentialError
  final case class UnexpectedError(msg: String) extends IssueCredentialError
  final case class UnsupportedDidFormat(did: String) extends IssueCredentialError
  final case class CreateCredentialPayloadFromRecordError(cause: Throwable) extends IssueCredentialError
  final case class CredentialIdNotDefined(credential: W3cCredentialPayload) extends IssueCredentialError
  final case class IrisError(cause: Throwable) extends IssueCredentialError
}
