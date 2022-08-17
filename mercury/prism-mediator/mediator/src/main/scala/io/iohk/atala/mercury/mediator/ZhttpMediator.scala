package io.iohk.atala.mercury.mediator

import zhttp.http._
import zhttp.service.Server
import zio._
import java.nio.charset.StandardCharsets

import io.iohk.atala.mercury.AgentService
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.Agent
import io.iohk.atala.mercury.DidComm

/** sbt "mediator/runMain io.iohk.atala.mercury.mediator.ZhttpMediator" */
object ZhttpMediator extends ZIOAppDefault {

  // Create HTTP route
  val app: HttpApp[DidComm & MailStorage, Throwable] = Http.collectZIO[Request] { case req @ Method.POST -> !! =>
    req.bodyAsString
      .flatMap(data => MediatorProgram.program(data))
      .map(str => Response.text(s"Done $str"))
  }

  override val run = { MediatorProgram.startLogo *> Server.start(MediatorProgram.port, app) }
    .provide(AgentService.mediator ++ MailStorage.layer)
}
