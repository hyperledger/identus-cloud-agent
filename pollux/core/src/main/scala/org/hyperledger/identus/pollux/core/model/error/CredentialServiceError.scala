package org.hyperledger.identus.pollux.core.model.error

import org.hyperledger.identus.pollux.core.model.DidCommID
import org.hyperledger.identus.pollux.vc.jwt.W3cCredentialPayload

import java.util.UUID

sealed trait CredentialServiceError {
  def toThrowable: Throwable = this match
    case CredentialServiceError.RepositoryError(cause)     => cause
    case CredentialServiceError.LinkSecretError(cause)     => cause
    case CredentialServiceError.RecordIdNotFound(recordId) => new Throwable(s"RecordId not found: $recordId")
    case CredentialServiceError.OperationNotExecuted(recordId, info) =>
      new Throwable(s"Operation not executed for recordId: $recordId, info: $info")
    case CredentialServiceError.ThreadIdNotFound(thid)     => new Throwable(s"ThreadId not found: $thid")
    case CredentialServiceError.InvalidFlowStateError(msg) => new Throwable(s"Invalid flow state: $msg")
    case CredentialServiceError.UnexpectedError(msg)       => new Throwable(s"Unexpected error: $msg")
    case CredentialServiceError.UnsupportedDidFormat(did)  => new Throwable(s"Unsupported DID format: $did")
    case CredentialServiceError.UnsupportedCredentialFormat(vcFormat) =>
      new Throwable(s"Unsupported credential format: $vcFormat")
    case CredentialServiceError.MissingCredentialFormat => new Throwable("Missing credential format")
    case CredentialServiceError.CreateCredentialPayloadFromRecordError(cause) => cause
    case CredentialServiceError.CredentialRequestValidationError(error) =>
      new Throwable(s"Credential request validation error: $error")
    case CredentialServiceError.CredentialIdNotDefined(credential) =>
      new Throwable(s"CredentialId not defined for credential: $credential")
    case CredentialServiceError.CredentialSchemaError(cause) =>
      new Throwable(s"Credential schema error: ${cause.message}")
    case CredentialServiceError.UnsupportedVCClaimsValue(error) => new Throwable(s"Unsupported VC claims value: $error")
    case CredentialServiceError.UnsupportedVCClaimsMediaType(media_type) =>
      new Throwable(s"Unsupported VC claims media type: $media_type")
    case CredentialServiceError.CredentialDefinitionPrivatePartNotFound(credentialDefinitionId) =>
      new Throwable(s"Credential definition private part not found: $credentialDefinitionId")
    case CredentialServiceError.CredentialDefinitionIdUndefined => new Throwable("Credential definition id undefined")
}

object CredentialServiceError {
  final case class RepositoryError(cause: Throwable) extends CredentialServiceError

  final case class LinkSecretError(cause: Throwable) extends CredentialServiceError
  final case class RecordIdNotFound(recordId: DidCommID) extends CredentialServiceError
  final case class OperationNotExecuted(recordId: DidCommID, info: String) extends CredentialServiceError
  final case class ThreadIdNotFound(thid: DidCommID) extends CredentialServiceError
  final case class InvalidFlowStateError(msg: String) extends CredentialServiceError
  final case class UnexpectedError(msg: String) extends CredentialServiceError
  final case class UnsupportedDidFormat(did: String) extends CredentialServiceError
  final case class UnsupportedCredentialFormat(vcFormat: String) extends CredentialServiceError
  object MissingCredentialFormat extends CredentialServiceError
  final case class CreateCredentialPayloadFromRecordError(cause: Throwable) extends CredentialServiceError
  final case class CredentialRequestValidationError(error: String) extends CredentialServiceError
  final case class CredentialIdNotDefined(credential: W3cCredentialPayload) extends CredentialServiceError
  final case class CredentialSchemaError(cause: org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError)
      extends CredentialServiceError
  final case class UnsupportedVCClaimsValue(error: String) extends CredentialServiceError
  final case class UnsupportedVCClaimsMediaType(media_type: String) extends CredentialServiceError
  final case class CredentialDefinitionPrivatePartNotFound(credentialDefinitionId: UUID) extends CredentialServiceError
  case object CredentialDefinitionIdUndefined extends CredentialServiceError
}
