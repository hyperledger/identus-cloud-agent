package io.iohk.atala.credentialstatus.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.credentialstatus.controller.http.CredentialStatusList
import io.iohk.atala.pollux.core.model.error.CredentialStatusListServiceError
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

trait CredentialStatusController {
  def getStatusListJwtCredentialById(id: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialStatusList]

}

object CredentialStatusController {
  def toHttpError(error: CredentialStatusListServiceError): ErrorResponse =
    error match
      case CredentialStatusListServiceError.RepositoryError(cause) =>
        ErrorResponse.internalServerError(title = "RepositoryError", detail = Some(cause.toString))
      case CredentialStatusListServiceError.RecordIdNotFound(recordId) =>
        ErrorResponse.notFound(detail = Some(s"Credential status list could not be found by id: $recordId"))

}
