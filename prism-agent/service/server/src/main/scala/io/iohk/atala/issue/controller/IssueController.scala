package io.iohk.atala.issue.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.issue.controller.http.{AcceptCredentialOfferRequest, IssueCredentialRecordPage, CreateIssueCredentialRecordRequest, IssueCredentialRecord}
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import zio.IO

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
    error match
      case CredentialServiceError.RepositoryError(cause) =>
        ErrorResponse.internalServerError(title = "RepositoryError", detail = Some(cause.toString))
      case CredentialServiceError.RecordIdNotFound(recordId) =>
        ErrorResponse.notFound(detail = Some(s"Record Id not found: $recordId"))
      case CredentialServiceError.OperationNotExecuted(recordId, info) => ??? //TODO
      case CredentialServiceError.ThreadIdNotFound(thid) =>
        ErrorResponse.notFound(detail = Some(s"Thread Id not found: $thid"))
      case CredentialServiceError.UnexpectedError(msg) =>
        ErrorResponse.internalServerError(detail = Some(msg))
      case CredentialServiceError.InvalidFlowStateError(msg) =>
        ErrorResponse.badRequest(title = "InvalidFlowState", detail = Some(msg))
      case CredentialServiceError.UnexpectedError(msg) => ??? //TODO
      case CredentialServiceError.UnsupportedDidFormat(msg) => ??? //TODO
      case CredentialServiceError.CreateCredentialPayloadFromRecordError(msg) => ??? //TODO
      case CredentialServiceError.CredentialRequestValidationError(msg) => ??? //TODO
      case CredentialServiceError.CredentialIdNotDefined(msg) => ??? //TODO
      case CredentialServiceError.IrisError(msg) => ??? //TODO
}
