package io.iohk.atala.connect.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.connect.controller.ConnectionEndpoints.*
import io.iohk.atala.connect.controller.http.CreateConnectionRequest
import io.iohk.atala.pollux.credentialschema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaController
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

class ConnectionServerEndpoints(connectionController: ConnectionController) {

  private val createConnectionServerEndpoint: ZServerEndpoint[Any, Any] =
    createConnection.zServerLogic { case (ctx: RequestContext, request: CreateConnectionRequest) =>
      connectionController.createConnection(request)(ctx)
    }

  val all: List[ZServerEndpoint[Any, Any]] = List(createConnectionServerEndpoint)
}

object ConnectionServerEndpoints {
  def all: URIO[ConnectionController, List[ZServerEndpoint[Any, Any]]] = {
    for {
      connectionController <- ZIO.service[ConnectionController]
      connectionEndpoints = new ConnectionServerEndpoints(connectionController)
    } yield connectionEndpoints.all
  }
}
