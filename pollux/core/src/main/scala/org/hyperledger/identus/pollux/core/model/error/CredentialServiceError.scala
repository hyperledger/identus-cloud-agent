package org.hyperledger.identus.pollux.core.model.error

import org.hyperledger.identus.pollux.core.model.DidCommID
import org.hyperledger.identus.pollux.vc.jwt.W3cCredentialPayload

import java.util.UUID

sealed trait CredentialServiceError

object CredentialServiceError {
  // To be removed one LinkSecretService is ADR compliant
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
