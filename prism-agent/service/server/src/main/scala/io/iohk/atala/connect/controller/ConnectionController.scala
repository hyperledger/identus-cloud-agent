package io.iohk.atala.connect.controller

import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.connect.controller.http.{
  AcceptConnectionInvitationRequest,
  Connection,
  ConnectionsPage,
  CreateConnectionRequest
}
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

trait ConnectionController {
  def createConnection(request: CreateConnectionRequest)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, Connection]

  def getConnection(connectionId: UUID)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, Connection]

  def getConnections(paginationInput: PaginationInput, thid: Option[String])(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, ConnectionsPage]

  def acceptConnectionInvitation(request: AcceptConnectionInvitationRequest)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, Connection]

}

object ConnectionController {
  def toHttpError(error: ConnectionServiceError): ErrorResponse =
    error match
      case ConnectionServiceError.RepositoryError(cause) =>
        ErrorResponse.internalServerError(title = "RepositoryError", detail = Some(cause.toString))
      case ConnectionServiceError.RecordIdNotFound(recordId) =>
        ErrorResponse.notFound(detail = Some(s"Record Id not found: $recordId"))
      case ConnectionServiceError.ThreadIdNotFound(thid) =>
        ErrorResponse.notFound(detail = Some(s"Thread Id not found: $thid"))
      case ConnectionServiceError.InvitationParsingError(cause) =>
        ErrorResponse.badRequest(title = "InvitationParsingError", detail = Some(cause.toString))
      case ConnectionServiceError.UnexpectedError(msg) =>
        ErrorResponse.internalServerError(detail = Some(msg))
      case ConnectionServiceError.InvalidFlowStateError(msg) =>
        ErrorResponse.badRequest(title = "InvalidFlowState", detail = Some(msg))
      case ConnectionServiceError.InvitationAlreadyReceived(msg) =>
        ErrorResponse.badRequest(title = "InvitationAlreadyReceived", detail = Some(msg))
}
