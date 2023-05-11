package io.iohk.atala.pollux.core.model.error

import io.iohk.atala.pollux.core.model.DidCommID
import io.iohk.atala.pollux.vc.jwt.W3cCredentialPayload

import java.util.UUID

sealed trait CredentialServiceError

object CredentialServiceError {
  final case class RepositoryError(cause: Throwable) extends CredentialServiceError
  final case class RecordIdNotFound(recordId: DidCommID) extends CredentialServiceError
  final case class OperationNotExecuted(recordId: DidCommID, info: String) extends CredentialServiceError
  final case class ThreadIdNotFound(thid: DidCommID) extends CredentialServiceError
  final case class InvalidFlowStateError(msg: String) extends CredentialServiceError
  final case class UnexpectedError(msg: String) extends CredentialServiceError
  final case class UnsupportedDidFormat(did: String) extends CredentialServiceError
  final case class CreateCredentialPayloadFromRecordError(cause: Throwable) extends CredentialServiceError
  final case class CredentialRequestValidationError(error: String) extends CredentialServiceError
  final case class CredentialIdNotDefined(credential: W3cCredentialPayload) extends CredentialServiceError
  final case class IrisError(cause: Throwable) extends CredentialServiceError
  final case class VCSchemaParsingError(error: String) extends CredentialServiceError
  final case class VCClaimsParsingError(cause: Throwable) extends CredentialServiceError
  final case class UnsupportedVCClaimsValue(error: String) extends CredentialServiceError
  final case class UnsupportedVCClaimsMimeType(mimeType: String) extends CredentialServiceError
  final case class VCClaimsValidationFailed(causes: Seq[String]) extends CredentialServiceError
}
