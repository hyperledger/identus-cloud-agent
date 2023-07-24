package io.iohk.atala.connect.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.connect.controller.ConnectionEndpoints.*
import io.iohk.atala.connect.controller.http.{AcceptConnectionInvitationRequest, CreateConnectionRequest}
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

import java.util.UUID

class ConnectionServerEndpoints(connectionController: ConnectionController, walletAccessCtx: WalletAccessContext) {

  private val createConnectionServerEndpoint: ZServerEndpoint[Any, Any] =
    createConnection.zServerLogic { case (ctx: RequestContext, request: CreateConnectionRequest) =>
      connectionController
        .createConnection(request)(ctx)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  private val getConnectionServerEndpoint: ZServerEndpoint[Any, Any] =
    getConnection.zServerLogic { case (ctx: RequestContext, connectionId: UUID) =>
      connectionController
        .getConnection(connectionId)(ctx)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  private val getConnectionsServerEndpoint: ZServerEndpoint[Any, Any] =
    getConnections.zServerLogic { case (ctx: RequestContext, paginationInput: PaginationInput, thid: Option[String]) =>
      connectionController
        .getConnections(paginationInput, thid)(ctx)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  private val acceptConnectionInvitationServerEndpoint: ZServerEndpoint[Any, Any] =
    acceptConnectionInvitation.zServerLogic { case (ctx: RequestContext, request: AcceptConnectionInvitationRequest) =>
      connectionController
        .acceptConnectionInvitation(request)(ctx)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    createConnectionServerEndpoint,
    getConnectionServerEndpoint,
    getConnectionsServerEndpoint,
    acceptConnectionInvitationServerEndpoint
  )
}

object ConnectionServerEndpoints {
  def all: URIO[ConnectionController & WalletAccessContext, List[ZServerEndpoint[Any, Any]]] = {
    for {
      // FIXME: do not use global wallet context, use context from interceptor instead
      walletAccessContext <- ZIO.service[WalletAccessContext]
      connectionController <- ZIO.service[ConnectionController]
      connectionEndpoints = new ConnectionServerEndpoints(connectionController, walletAccessContext)
    } yield connectionEndpoints.all
  }
}
