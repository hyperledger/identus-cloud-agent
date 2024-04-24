package org.hyperledger.identus.presentproof.controller

import org.hyperledger.identus.api.http.model.PaginationInput
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.pollux.core.model.error.PresentationError
import org.hyperledger.identus.presentproof.controller.http.*
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.ZIO

import java.util.UUID
import scala.util.Try

trait PresentProofController {
  def requestPresentation(
      requestPresentationInput: RequestPresentationInput
  )(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, PresentationStatus]

  def getPresentations(
      paginationInput: PaginationInput,
      thid: Option[String]
  )(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, PresentationStatusPage]

  def getPresentation(id: UUID)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, PresentationStatus]

  def updatePresentation(id: UUID, requestPresentationAction: RequestPresentationAction)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, PresentationStatus]

}

object PresentProofController {
  def toHttpError(error: PresentationError): ErrorResponse =
    error match
      case PresentationError.RepositoryError(cause) =>
        ErrorResponse.internalServerError(title = "RepositoryError", detail = Some(cause.toString))
      case PresentationError.RecordIdNotFound(recordId) =>
        ErrorResponse.notFound(detail = Some(s"Record Id not found: $recordId"))
      case PresentationError.ThreadIdNotFound(thid) =>
        ErrorResponse.notFound(detail = Some(s"Thread Id not found: $thid"))
      case PresentationError.InvalidFlowStateError(msg) =>
        ErrorResponse.badRequest(title = "InvalidFlowState", detail = Some(msg))
      case PresentationError.MissingAnoncredPresentationRequest(msg) =>
        ErrorResponse.badRequest(title = "Missing Anoncred Presentation Request", detail = Some(msg))
      case PresentationError.AnoncredPresentationCreationError(cause) =>
        ErrorResponse.badRequest(title = "Error Creating Anoncred Presentation", detail = Some(cause.toString))
      case PresentationError.AnoncredPresentationVerificationError(cause) =>
        ErrorResponse.badRequest(title = "Error Verifying Prensetation", detail = Some(cause.toString))
      case PresentationError.InvalidAnoncredPresentationRequest(msg) =>
        ErrorResponse.badRequest(title = "Invalid Anoncred Presentation Request", detail = Some(msg))
      case PresentationError.InvalidAnoncredPresentation(msg) =>
        ErrorResponse.badRequest(title = "Invalid Anoncred Presentation", detail = Some(msg))
      case PresentationError.NotMatchingPresentationCredentialFormat(cause) =>
        ErrorResponse.badRequest(
          title = "Presentation and Credential Format Not Matching",
          detail = Some(cause.toString)
        )
      case PresentationError.UnexpectedError(msg) =>
        ErrorResponse.internalServerError(detail = Some(msg))
      case PresentationError.IssuedCredentialNotFoundError(_) =>
        ErrorResponse.internalServerError(detail = Some("Issued credential not found"))
      case PresentationError.PresentationDecodingError(_) =>
        ErrorResponse.internalServerError(detail = Some("Presentation decoding error"))
      case PresentationError.PresentationNotFoundError(_) =>
        ErrorResponse.notFound(detail = Some("Presentation no found"))
      case PresentationError.HolderBindingError(msg) =>
        ErrorResponse.internalServerError(detail = Some(s"Holder binding error: $msg"))
      case PresentationError.MissingCredential =>
        ErrorResponse.badRequest(
          title = "MissingCredential",
          detail = Some("The Credential is missing from attachments")
        )
      case PresentationError.MissingCredentialFormat =>
        ErrorResponse.badRequest(
          title = "MissingCredentialFormat",
          detail = Some("The Credential format is missing from the credential in attachment")
        )
      case PresentationError.UnsupportedCredentialFormat(format) =>
        ErrorResponse.badRequest(
          title = "UnsupportedCredentialFormat",
          detail = Some(s"The Credential format '$format' is not Unsupported")
        )

  def toDidCommID(str: String): ZIO[Any, ErrorResponse, org.hyperledger.identus.pollux.core.model.DidCommID] =
    ZIO
      .fromTry(Try(org.hyperledger.identus.pollux.core.model.DidCommID(str)))
      .mapError(e => ErrorResponse.badRequest(s"Error parsing string as DidCommID: ${e.getMessage}"))
}
