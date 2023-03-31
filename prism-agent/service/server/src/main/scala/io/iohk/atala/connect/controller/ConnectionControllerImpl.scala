package io.iohk.atala.connect.controller

import io.iohk.atala.agent.server.config.AgentConfig
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.connect.controller.http.{Connection, CreateConnectionRequest}
import io.iohk.atala.connect.core.service.ConnectionService
import zio.IO

class ConnectionControllerImpl(
    service: ConnectionService,
    managedDIDService: ManagedDIDService,
    agentConfig: AgentConfig
) extends ConnectionController {

  override def createConnection(request: CreateConnectionRequest)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, Connection] = {
    val result = for {
      pairwiseDid <- managedDIDService
        .createAndStorePeerDID(agentConfig.didCommServiceEndpointUrl)
      connection <- service.createConnectionInvitation(request.label, pairwiseDid.did)
    } yield Connection.fromDomain(connection)

    result.mapError(_ => ErrorResponse(500, "", "", None, ""))
  }

}
