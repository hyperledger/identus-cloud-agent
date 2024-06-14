package org.hyperledger.identus.pollux.core.model.error

import org.hyperledger.identus.pollux.core.model.schema.validator.JsonSchemaError
import org.hyperledger.identus.pollux.core.model.DidCommID
import org.hyperledger.identus.pollux.core.service.URIDereferencerError
import org.hyperledger.identus.shared.models.{Failure, StatusCode}

sealed trait PresentationError(
    val statusCode: StatusCode,
    val userFacingMessage: String
) extends Failure {
  override val namespace = "Presentation"
}

object PresentationError {

  // TODO: Remove once PresentationJob is cleaned
  final case class UnexpectedError(error: String)
      extends PresentationError(
        StatusCode.InternalServerError,
        error
      )

  final case class RecordIdNotFound(recordId: DidCommID)
      extends PresentationError(
        StatusCode.NotFound,
        s"Record Id not found: $recordId"
      )

  final case class ThreadIdNotFound(thid: DidCommID)
      extends PresentationError(
        StatusCode.NotFound,
        s"Thread Id not found: $thid"
      )

  final case class NoThreadIdFoundInRecord(presentationId: String)
      extends PresentationError(
        StatusCode.InternalServerError,
        s"Presentation record has missing ThreadId for record: $presentationId"
      )

  final case class InvalidFlowStateError(msg: String)
      extends PresentationError(
        StatusCode.BadRequest,
        msg
      )

  final case class RequestPresentationHasMultipleAttachment(presentationId: String)
      extends PresentationError(
        StatusCode.BadRequest,
        s"Request Presentation with multi attachments: $presentationId"
      )

  final case class IssuedCredentialNotFoundError(cause: String)
      extends PresentationError(
        StatusCode.InternalServerError,
        "Issued credential not found"
      )

  final case class InvalidSchemaURIError(schemaUri: String, error: Throwable)
      extends PresentationError(
        StatusCode.BadRequest,
        s"Invalid Schema Uri: $schemaUri, Error: ${error.getMessage}"
      )

  final case class InvalidCredentialDefinitionURIError(credentialDefinitionUri: String, error: Throwable)
      extends PresentationError(
        StatusCode.BadRequest,
        s"Invalid Credential Definition Uri: $credentialDefinitionUri, Error: ${error.getMessage}"
      )

  final case class SchemaURIDereferencingError(error: URIDereferencerError)
      extends PresentationError(
        error.statusCode,
        error.userFacingMessage
      )

  final case class CredentialDefinitionURIDereferencingError(error: URIDereferencerError)
      extends PresentationError(
        error.statusCode,
        error.userFacingMessage
      )

  final case class PresentationDecodingError(cause: String)
      extends PresentationError(
        StatusCode.InternalServerError,
        s"Presentation decoding error: $cause"
      )

  final case class HolderBindingError(msg: String)
      extends PresentationError(
        StatusCode.InternalServerError,
        s"Holder binding error: $msg"
      )

  object MissingCredential
      extends PresentationError(
        StatusCode.BadRequest,
        s"The Credential is missing from attachments"
      )

  object MissingCredentialFormat
      extends PresentationError(
        StatusCode.BadRequest,
        s"The Credential format is missing from the credential in attachment"
      )

  final case class UnsupportedCredentialFormat(vcFormat: String)
      extends PresentationError(
        StatusCode.BadRequest,
        s"The Credential format '$vcFormat' is not Unsupported"
      )

  final case class InvalidAnoncredPresentationRequest(error: String)
      extends PresentationError(
        StatusCode.InternalServerError,
        error
      )

  final case class InvalidAnoncredPresentation(error: String)
      extends PresentationError(
        StatusCode.InternalServerError,
        error
      )

  final case class MissingAnoncredPresentationRequest(error: String)
      extends PresentationError(
        StatusCode.InternalServerError,
        error
      )

  final case class NotMatchingPresentationCredentialFormat(cause: Throwable)
      extends PresentationError(
        StatusCode.BadRequest,
        s"Presentation and Credential Format Not Matching: ${cause.toString}"
      )

  final case class AnoncredPresentationCreationError(cause: Throwable)
      extends PresentationError(
        StatusCode.InternalServerError,
        cause.toString
      )

  final case class AnoncredCredentialProofParsingError(cause: String)
      extends PresentationError(
        StatusCode.BadRequest,
        cause
      )

  final case class AnoncredPresentationParsingError(cause: JsonSchemaError)
      extends PresentationError(
        StatusCode.BadRequest,
        cause.error
      )

  final case class AnoncredPresentationVerificationError(cause: Throwable)
      extends PresentationError(
        StatusCode.BadRequest,
        cause.toString
      )
}
