package io.iohk.atala.connect.controller

import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.connect.controller.http.{
  AcceptConnectionInvitationRequest,
  Connection,
  ConnectionsPage,
  CreateConnectionRequest
}
import org.hyperledger.identus.shared.models.WalletAccessContext
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
