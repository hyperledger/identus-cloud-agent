package org.hyperledger.identus.issue.controller

import org.hyperledger.identus.api.http.model.PaginationInput
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
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
      case CredentialServiceError.RepositoryError(cause) =>
        ErrorResponse.internalServerError(title = "RepositoryError", detail = Some(cause.toString))
      case CredentialServiceError.LinkSecretError(cause) =>
        ErrorResponse.internalServerError(title = "LinkSecretError", detail = Some(cause.toString))
      case CredentialServiceError.RecordIdNotFound(recordId) =>
        ErrorResponse.notFound(detail = Some(s"Record Id not found: $recordId"))
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
      case CredentialServiceError.CredentialSchemaError(e) =>
        ErrorResponse.badRequest(title = "Credential Schema Error", detail = Some(e.message))
      case CredentialServiceError.UnsupportedVCClaimsValue(error) =>
        ErrorResponse.badRequest(detail = Some(error))
      case CredentialServiceError.UnsupportedVCClaimsMediaType(media_type) =>
        ErrorResponse.badRequest(detail = Some(s"Unsupported media_type for claim: $media_type"))
      case CredentialServiceError.UnsupportedCredentialFormat(format) =>
        ErrorResponse.badRequest(detail = Some(s"Unsupported format in claim: $format"))
      case CredentialServiceError.MissingCredentialFormat =>
        ErrorResponse.badRequest(detail = Some(s"Missing credential format in claim"))
      case CredentialServiceError.CredentialDefinitionPrivatePartNotFound(credentialDefinitionId) =>
        ErrorResponse.badRequest(detail =
          Some(s"Credential Definition (id: $credentialDefinitionId) private part not found")
        )
      case CredentialServiceError.CredentialDefinitionIdUndefined =>
        ErrorResponse.badRequest(detail = Some(s"Credential Definition id undefined"))

}
