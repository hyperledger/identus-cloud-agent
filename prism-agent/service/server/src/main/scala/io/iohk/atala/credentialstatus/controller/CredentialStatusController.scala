package io.iohk.atala.credentialstatus.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.credentialstatus.controller.http.StatusListCredential
import io.iohk.atala.pollux.core.model.error.CredentialStatusListServiceError
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import io.iohk.atala.pollux.core.model.DidCommID

import java.util.UUID

trait CredentialStatusController {
  def getStatusListCredentialById(id: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, StatusListCredential]

  def revokeCredentialById(id: DidCommID)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, Unit]

}

object CredentialStatusController {
  def toHttpError(error: CredentialStatusListServiceError): ErrorResponse =
    error match
      case CredentialStatusListServiceError.RepositoryError(cause) =>
        ErrorResponse.internalServerError(title = "RepositoryError", detail = Some(cause.toString))
      case CredentialStatusListServiceError.JsonCredentialParsingError(cause) =>
        ErrorResponse.internalServerError(title = "JsonCredentialParsingError", detail = Some(cause.toString))
      case CredentialStatusListServiceError.RecordIdNotFound(recordId) =>
        ErrorResponse.notFound(detail = Some(s"Credential status list could not be found by id: $recordId"))
      case CredentialStatusListServiceError.IssueCredentialRecordNotFound(issueCredentialRecordId: DidCommID) =>
        ErrorResponse.notFound(detail =
          Some(s"Credential with id $issueCredentialRecordId is either already revoked or does not exist")
        )

}
