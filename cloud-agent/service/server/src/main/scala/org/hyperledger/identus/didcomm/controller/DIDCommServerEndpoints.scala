package org.hyperledger.identus.didcomm.controller

import org.hyperledger.identus.api.http.RequestContext
import org.hyperledger.identus.didcomm.controller.http.DIDCommMessage
import sttp.tapir.ztapir.RichZEndpoint
import sttp.tapir.ztapir.ZServerEndpoint
import zio.URIO
import zio.ZIO

class DIDCommServerEndpoints(
    didCommController: DIDCommController
) {
  private val handleDIDCommMessageServerEndpoint: ZServerEndpoint[Any, Any] = DIDCommEndpoints.handleDIDCommMessage
    .zServerLogic { case (ctx: RequestContext, msg: DIDCommMessage) =>
      didCommController.handleDIDCommMessage(msg)(ctx)
    }

  val all: List[ZServerEndpoint[Any, Any]] = List(handleDIDCommMessageServerEndpoint)
}

object DIDCommServerEndpoints {
  def all: URIO[DIDCommController, List[ZServerEndpoint[Any, Any]]] = {
    for {
      didCommController <- ZIO.service[DIDCommController]
      didCommEndpoints = new DIDCommServerEndpoints(didCommController)
    } yield didCommEndpoints.all
  }
}
