package io.iohk.atala.pollux.core.model.error

import java.util.UUID
import io.iohk.atala.pollux.vc.jwt.W3cCredentialPayload

sealed trait CredentialServiceError

object CredentialServiceError {
  final case class RepositoryError(cause: Throwable) extends CredentialServiceError
  final case class RecordIdNotFound(recordId: UUID) extends CredentialServiceError
  final case class OperationNotExecuted(recordId: UUID, info: String) extends CredentialServiceError
  final case class ThreadIdNotFound(thid: UUID) extends CredentialServiceError
  final case class InvalidFlowStateError(msg: String) extends CredentialServiceError
  final case class UnexpectedError(msg: String) extends CredentialServiceError
  final case class UnsupportedDidFormat(did: String) extends CredentialServiceError
  final case class CreateCredentialPayloadFromRecordError(cause: Throwable) extends CredentialServiceError
  final case class CredentialIdNotDefined(credential: W3cCredentialPayload) extends CredentialServiceError
  final case class IrisError(cause: Throwable) extends CredentialServiceError
}
