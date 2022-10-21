package io.iohk.atala.mercury.mediator

import zhttp.http._
import zhttp.service.Server
import zio._
import java.nio.charset.StandardCharsets

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.{Agent, DidComm, InvitationPrograms, MediaTypes}
import io.iohk.atala.mercury.resolvers.MediatorDidComm
import scala.io.Source

/** sbt "mediator/runMain io.iohk.atala.mercury.mediator.ZhttpMediator" */
object ZhttpMediator extends ZIOAppDefault {
  val header = "content-type" -> MediaTypes.contentTypeEncrypted

  // Create HTTP route
  val app: HttpApp[DidComm & MailStorage & ConnectionStorage, Throwable] = Http.collectZIO[Request] {
    case req @ Method.POST -> !!
        if req.headersAsList.exists(h => h._1.equalsIgnoreCase(header._1) && h._2.equalsIgnoreCase(header._2)) =>
      req.bodyAsString
        .flatMap(data => MediatorProgram.program(data))
        .map(str => Response.text(str))
    case Method.GET -> !! / "api" / "openapi-spec.yaml" =>
      ZIO.succeed(
        Response.text(
          Source.fromResource("mercury-mediator-openapi.yaml").iter.mkString
        )
      )
    case req @ Method.GET -> !! / "oob_url" =>
      val serverUrl = s"http://locahost:${MediatorProgram.port}?_oob="
      InvitationPrograms.createInvitationV2().map(oob => Response.text(serverUrl + oob))

    case req =>
      ZIO.succeed(
        Response.text(
          s"The request must be a POST to root with the Header $header"
        )
      )
  }

  override val run = { MediatorProgram.startLogo *> Server.start(MediatorProgram.port, app) }
    .provide(MediatorDidComm.peerDidMediator ++ MailStorage.layer ++ ConnectionStorage.layer)
}
