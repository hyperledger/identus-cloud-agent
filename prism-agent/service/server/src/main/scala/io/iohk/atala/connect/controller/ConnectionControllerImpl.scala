package io.iohk.atala.connect.controller

import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.connect.controller.ConnectionController.toHttpError
import io.iohk.atala.connect.controller.http.{
  AcceptConnectionInvitationRequest,
  Connection,
  ConnectionsPage,
  CreateConnectionRequest
}
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.service.ConnectionService
import zio.*

import java.util.UUID

class ConnectionControllerImpl(
    service: ConnectionService,
    managedDIDService: ManagedDIDService,
    appConfig: AppConfig
) extends ConnectionController {

  override def createConnection(request: CreateConnectionRequest)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, Connection] = {
    val result = for {
      pairwiseDid <- managedDIDService.createAndStorePeerDID(appConfig.agent.didCommServiceEndpointUrl)
      connection <- service.createConnectionInvitation(request.label, pairwiseDid.did)
    } yield Connection.fromDomain(connection)

    result.mapError(toHttpError)
  }

  override def getConnection(
      connectionId: UUID
  )(implicit rc: RequestContext): IO[ErrorResponse, Connection] = {
    val result = for {
      maybeConnection <- service.getConnectionRecord(connectionId)
      connection <- ZIO
        .fromOption(maybeConnection)
        .mapError(_ => ConnectionServiceError.RecordIdNotFound(connectionId))
    } yield Connection.fromDomain(connection)

    result.mapError(toHttpError)
  }

  override def getConnections(
      paginationInput: PaginationInput,
      thid: Option[String]
  )(implicit rc: RequestContext): IO[ErrorResponse, ConnectionsPage] = {
    val result = for {
      connections <- thid match
        case None       => service.getConnectionRecords()
        case Some(thid) => service.getConnectionRecordByThreadId(thid).map(_.toSeq)
    } yield ConnectionsPage(contents = connections.map(Connection.fromDomain))

    result.mapError(toHttpError)
  }

  override def acceptConnectionInvitation(
      request: AcceptConnectionInvitationRequest
  )(implicit rc: RequestContext): IO[ErrorResponse, Connection] = {
    val result = for {
      record <- service.receiveConnectionInvitation(request.invitation)
      pairwiseDid <- managedDIDService.createAndStorePeerDID(appConfig.agent.didCommServiceEndpointUrl)
      connection <- service.acceptConnectionInvitation(record.id, pairwiseDid.did)
    } yield Connection.fromDomain(connection)

    result.mapError(toHttpError)
  }
}

object ConnectionControllerImpl {
  val layer: URLayer[ConnectionService & ManagedDIDService & AppConfig, ConnectionController] =
    ZLayer.fromFunction(ConnectionControllerImpl(_, _, _))
}
