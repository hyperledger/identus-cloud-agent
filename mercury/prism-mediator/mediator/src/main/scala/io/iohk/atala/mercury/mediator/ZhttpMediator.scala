package io.iohk.atala.mercury.mediator

import zhttp.http._
import zhttp.service.Server
import zio._
import java.nio.charset.StandardCharsets

import io.iohk.atala.mercury.AgentService
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.Agent
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.mercury.MediaTypes

/** sbt "mediator/runMain io.iohk.atala.mercury.mediator.ZhttpMediator" */
object ZhttpMediator extends ZIOAppDefault {
  val header = "content-type" -> MediaTypes.contentTypeEncrypted

  // Create HTTP route
  val app: HttpApp[DidComm & MailStorage, Throwable] = Http.collectZIO[Request] {
    case req @ Method.POST -> !!
        if req.headersAsList.exists(h => h._1.equalsIgnoreCase(header._1) && h._2.equalsIgnoreCase(header._2)) =>
      req.bodyAsString
        .flatMap(data => MediatorProgram.program(data))
        .map(str => Response.text(s"Done $str"))
    case req =>
      ZIO.succeed(
        Response.text(
          s"The request must be a POST to root with the Header $header"
        )
      )
  }

  override val run = { MediatorProgram.startLogo *> Server.start(MediatorProgram.port, app) }
    .provide(AgentService.mediator ++ MailStorage.layer)
}
