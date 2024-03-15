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
import scala.util.matching.Regex

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
  private val CamelCaseSplitRegex: Regex = "(([A-Z]?[a-z]+)|([A-Z]))".r

  def toHttpError(error: ConnectionServiceError): ErrorResponse =
    val simpleName = error.getClass.getSimpleName
    ErrorResponse(
      error.statusCode.code,
      s"error:ConnectionServiceError:$simpleName",
      CamelCaseSplitRegex.findAllIn(simpleName).mkString(" "),
      Some(error.userFacingMessage)
    )
}
