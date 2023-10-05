package io.iohk.atala.connect.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.connect.controller.ConnectionEndpoints.*
import io.iohk.atala.connect.controller.http.{AcceptConnectionInvitationRequest, CreateConnectionRequest}
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

import java.util.UUID

class ConnectionServerEndpoints(connectionController: ConnectionController, authenticator: Authenticator) {

  private val createConnectionServerEndpoint: ZServerEndpoint[Any, Any] =
    createConnection
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, request: CreateConnectionRequest) =>
          connectionController
            .createConnection(request)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  private val getConnectionServerEndpoint: ZServerEndpoint[Any, Any] =
    getConnection
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, connectionId: UUID) =>
          connectionController
            .getConnection(connectionId)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  private val getConnectionsServerEndpoint: ZServerEndpoint[Any, Any] =
    getConnections
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, paginationInput: PaginationInput, thid: Option[String]) =>
          connectionController
            .getConnections(paginationInput, thid)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  private val acceptConnectionInvitationServerEndpoint: ZServerEndpoint[Any, Any] =
    acceptConnectionInvitation
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, request: AcceptConnectionInvitationRequest) =>
          connectionController
            .acceptConnectionInvitation(request)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    createConnectionServerEndpoint,
    getConnectionServerEndpoint,
    getConnectionsServerEndpoint,
    acceptConnectionInvitationServerEndpoint
  )
}

object ConnectionServerEndpoints {
  def all: URIO[ConnectionController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
      connectionController <- ZIO.service[ConnectionController]
      connectionEndpoints = new ConnectionServerEndpoints(connectionController, authenticator)
    } yield connectionEndpoints.all
  }
}
