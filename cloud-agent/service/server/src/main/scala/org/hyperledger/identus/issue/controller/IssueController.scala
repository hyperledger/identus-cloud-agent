package org.hyperledger.identus.issue.controller

import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.api.http.model.PaginationInput
import org.hyperledger.identus.issue.controller.http.{
  AcceptCredentialOfferRequest,
  CreateIssueCredentialRecordRequest,
  IssueCredentialRecord,
  IssueCredentialRecordPage
}
import org.hyperledger.identus.pollux.core.model.error.CredentialServiceError
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

trait IssueController {
  def createCredentialOffer(request: CreateIssueCredentialRecordRequest)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecord]

  def getCredentialRecords(paginationInput: PaginationInput, thid: Option[String])(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecordPage]

  def getCredentialRecord(recordId: String)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecord]

  def acceptCredentialOffer(recordId: String, request: AcceptCredentialOfferRequest)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecord]

  def issueCredential(recordId: String)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecord]

}

object IssueController {
  def toHttpError(error: CredentialServiceError): ErrorResponse =
    error match
      case CredentialServiceError.RecordIdNotFound(recordId) =>
        ErrorResponse.notFound(detail = Some(s"Record Id not found: $recordId"))
      case CredentialServiceError.UnexpectedError(msg) =>
        ErrorResponse.internalServerError(detail = Some(msg))
      case CredentialServiceError.CreateCredentialPayloadFromRecordError(msg) =>
        ErrorResponse.badRequest(title = "Create Credential Payload From Record Error", detail = Some(msg.getMessage))
      case CredentialServiceError.CredentialRequestValidationError(msg) =>
        ErrorResponse.badRequest(title = "Create Request Validation Error", detail = Some(msg))
      case CredentialServiceError.CredentialSchemaError(e) =>
        ErrorResponse.badRequest(title = "Credential Schema Error", detail = Some(e.userFacingMessage))
      case CredentialServiceError.UnsupportedCredentialFormat(format) =>
        ErrorResponse.badRequest(detail = Some(s"Unsupported format in claim: $format"))
      case CredentialServiceError.MissingCredentialFormat =>
        ErrorResponse.badRequest(detail = Some(s"Missing credential format in claim"))

}
