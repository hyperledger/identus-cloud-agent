package io.iohk.atala.mercury.mediator

import zhttp.http._
import zhttp.service.Server
import zio._
import java.nio.charset.StandardCharsets

import io.iohk.atala.mercury.AgentService
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.Agent

/** sbt "mediator/runMain io.iohk.atala.mercury.mediator.ZhttpMediator" */
object ZhttpMediator extends ZIOAppDefault {

  // Create HTTP route
  val app: HttpApp[Any, Throwable] = Http.collectZIO[Request] { case req @ Method.POST -> !! =>
    req.bodyAsString
      .flatMap(data => MediatorProgram.program(data))
      .map(unit => Response.text("Done"))
      .provideLayer(AgentService.mediator ++ MyDB.live)
  // .provideLayer(MyDB.live)

  }

  override val run = MediatorProgram.startLogo *> Server.start(MediatorProgram.port, app)
}
