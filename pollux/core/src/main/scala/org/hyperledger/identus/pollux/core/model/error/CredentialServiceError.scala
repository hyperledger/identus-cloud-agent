package org.hyperledger.identus.pollux.core.model.error

import org.hyperledger.identus.pollux.core.model.DidCommID
import org.hyperledger.identus.shared.models.{Failure, StatusCode}

import java.util.UUID

sealed trait CredentialServiceError

object CredentialServiceError {
  final case class RecordIdNotFound(recordId: DidCommID) extends CredentialServiceError
  final case class OperationNotExecuted(recordId: DidCommID, info: String) extends CredentialServiceError
  final case class ThreadIdNotFound(thid: DidCommID) extends CredentialServiceError
  final case class InvalidFlowStateError(msg: String) extends CredentialServiceError
  final case class UnexpectedError(msg: String) extends CredentialServiceError
  final case class UnsupportedCredentialFormat(vcFormat: String) extends CredentialServiceError
  object MissingCredentialFormat extends CredentialServiceError
  final case class CreateCredentialPayloadFromRecordError(cause: Throwable) extends CredentialServiceError
  final case class CredentialRequestValidationError(error: String) extends CredentialServiceError
  final case class CredentialSchemaError(cause: org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError)
      extends CredentialServiceError
  final case class UnsupportedVCClaimsValue(error: String) extends CredentialServiceError
  final case class UnsupportedVCClaimsMediaType(media_type: String) extends CredentialServiceError
}

sealed trait CredentialServiceErrorNew(
    val statusCode: StatusCode,
    val userFacingMessage: String
) extends Failure
    with CredentialServiceError {
  override val namespace: String = "CredentialServiceError"
}

object CredentialServiceErrorNew {
  final case class InvalidCredentialOffer(cause: String)
      extends CredentialServiceErrorNew(
        StatusCode.BadRequest,
        s"The credential offer is invalid: cause[$cause]"
      )

  final case class UnsupportedDidFormat(did: String)
      extends CredentialServiceErrorNew(
        StatusCode.UnprocessableContent,
        s"The DID format is not supported: did=$did"
      )

  final case class CredentialDefinitionServiceError(cause: String)
      extends CredentialServiceErrorNew(
        StatusCode.InternalServerError,
        s"An error occurred related the credential definition: cause[$cause]"
      )

  final case class CredentialDefinitionPrivatePartNotFound(guid: UUID)
      extends CredentialServiceErrorNew(
        StatusCode.NotFound,
        s"There is no private part matching the credential definition: guid=$guid"
      )
}
