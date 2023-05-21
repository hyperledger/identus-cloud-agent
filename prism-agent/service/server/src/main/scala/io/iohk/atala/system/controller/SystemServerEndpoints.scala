package io.iohk.atala.system.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.system.controller.SystemEndpoints.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

class SystemServerEndpoints(systemController: SystemController) {

  val healthEndpoint: ZServerEndpoint[Any, Any] =
    health.zServerLogic { case (ctx: RequestContext) =>
      systemController.health()(ctx)
    }

  val metricsEndpoint: ZServerEndpoint[Any, Any] =
    metrics.zServerLogic { case (ctx: RequestContext) =>
      systemController.metrics()(ctx)
    }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    healthEndpoint,
    metricsEndpoint
  )

}

object SystemServerEndpoints {
  def all: URIO[SystemController, List[ZServerEndpoint[Any, Any]]] = {
    for {
      systemController <- ZIO.service[SystemController]
      systemServerEndpoints = new SystemServerEndpoints(systemController)
    } yield systemServerEndpoints.all
  }
}
