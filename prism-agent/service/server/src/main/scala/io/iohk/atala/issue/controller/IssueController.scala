package io.iohk.atala.issue.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.issue.controller.http.{AcceptCredentialOfferRequest, CreateIssueCredentialRecordRequest, IssueCredentialRecord, IssueCredentialRecordPage}
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import zio.{IO, ZIO}

trait IssueController {
  def createCredentialOffer(request: CreateIssueCredentialRecordRequest)(implicit
                                                                         rc: RequestContext
  ): IO[ErrorResponse, IssueCredentialRecord]

  def getCredentialRecords(paginationInput: PaginationInput, thid: Option[String])(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecordPage]

  def getCredentialRecord(recordId: String)(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord]

  def acceptCredentialOffer(recordId: String, request: AcceptCredentialOfferRequest)(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord]

  def issueCredential(recordId: String)(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord]

}

object IssueController {
  def toHttpError(error: CredentialServiceError): ErrorResponse =
    ZIO.logError(toHttpError(error).title)
    error match
      case CredentialServiceError.RepositoryError(cause) =>
        ErrorResponse.internalServerError(title = "RepositoryError", detail = Some(cause.toString))
      case CredentialServiceError.RecordIdNotFound(recordId) =>
        ErrorResponse.notFound("NotFound", Some(s"Record Id not found: $recordId"), "")
//        ErrorResponse.notFound(detail = Some(s"Record Id not found: $recordId"))
//        ErrorResponse.badRequest("Unsupported DID format", Some(s"The following DID is not supported:  $recordId"))
      case CredentialServiceError.OperationNotExecuted(recordId, info) =>
        ErrorResponse.internalServerError(title = "Operation Not Executed", detail = Some(s"${recordId}-${info}"))
      case CredentialServiceError.ThreadIdNotFound(thid) =>
        ErrorResponse.notFound(detail = Some(s"Thread Id not found: $thid"))
      case CredentialServiceError.UnexpectedError(msg) =>
        ErrorResponse.internalServerError(detail = Some(msg))
      case CredentialServiceError.InvalidFlowStateError(msg) =>
        ErrorResponse.badRequest(title = "InvalidFlowState", detail = Some(msg))
      case CredentialServiceError.UnsupportedDidFormat(did) =>
        ErrorResponse.badRequest("Unsupported DID format", Some(s"The following DID is not supported: ${did}"))
      case CredentialServiceError.CreateCredentialPayloadFromRecordError(msg) =>
        ErrorResponse.badRequest(title = "Create Credential Payload From Record Error", detail = Some(msg.getMessage))
      case CredentialServiceError.CredentialRequestValidationError(msg) =>
        ErrorResponse.badRequest(title = "Create Request Validation Error", detail = Some(msg))
      case CredentialServiceError.CredentialIdNotDefined(msg) =>
        ErrorResponse.badRequest(title = "Credential ID not defined one request", detail = Some(msg.toString))
      case CredentialServiceError.IrisError(msg) =>
        ErrorResponse.internalServerError(title = "VDR Error", detail = Some(msg.toString))
}
