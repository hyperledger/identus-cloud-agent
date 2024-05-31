package org.hyperledger.identus.connect.controller

import org.hyperledger.identus.api.http.model.PaginationInput
import org.hyperledger.identus.api.http.ErrorResponse
import org.hyperledger.identus.api.http.RequestContext
import org.hyperledger.identus.connect.controller.http.AcceptConnectionInvitationRequest
import org.hyperledger.identus.connect.controller.http.Connection
import org.hyperledger.identus.connect.controller.http.ConnectionsPage
import org.hyperledger.identus.connect.controller.http.CreateConnectionRequest
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
