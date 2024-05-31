package org.hyperledger.identus.system.controller

import org.hyperledger.identus.api.http.RequestContext
import org.hyperledger.identus.system.controller.SystemEndpoints._
import org.hyperledger.identus.LogUtils._
import sttp.tapir.ztapir._
import zio.{URIO, ZIO}

class SystemServerEndpoints(systemController: SystemController) {

  val healthEndpoint: ZServerEndpoint[Any, Any] =
    health.zServerLogic { case (ctx: RequestContext) =>
      systemController
        .health()(ctx)
        .logTrace(ctx)
    }

  val metricsEndpoint: ZServerEndpoint[Any, Any] =
    metrics.zServerLogic { case (ctx: RequestContext) =>
      systemController
        .metrics()(ctx)
        .logTrace(ctx)
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
