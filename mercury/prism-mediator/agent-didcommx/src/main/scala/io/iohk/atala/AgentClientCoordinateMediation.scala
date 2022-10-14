package io.iohk.atala

import zio._
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zhttp.http.{Headers, HttpData, Method}
import io.iohk.atala.mercury.Agent
import io.iohk.atala.mercury.AgentService
import io.iohk.atala.mercury.MediaTypes
import io.iohk.atala.mercury.model.UnpackMesage
import io.iohk.atala.mercury.protocol.mailbox.Mailbox.ReadMessage
import org.didcommx.didcomm.message.Attachment.Data.Json

import io.iohk.atala.mercury.protocol.coordinatemediation._
import io.iohk.atala.mercury.CoordinateMediationPrograms
import io.iohk.atala.mercury.InvitationPrograms

@main def AgentClientGetInvitation() = {
  val env = ChannelFactory.auto ++ EventLoopGroup.auto()
  val app =
    InvitationPrograms.getInvitationProgram("http://localhost:8000/oob_url").provide(env)

  Unsafe.unsafe { Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}

@main def AgentClientCoordinateMediationWithRootsId() = {
  val env = ChannelFactory.auto ++ EventLoopGroup.auto()
  val mediatorURL = "http://localhost:8000"
  val app = CoordinateMediationPrograms
    .senderMediationRequestProgram(mediatorURL)
    .provide(env, AgentService.charlie)

  Unsafe.unsafe { Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}

@main def AgentClientCoordinateMediation() = {
  val env = ChannelFactory.auto ++ EventLoopGroup.auto()
  val mediatorURL = "http://localhost:8080"
  val app = CoordinateMediationPrograms
    .senderMediationRequestProgram(mediatorURL)
    .provide(env, AgentService.charlie)

  Unsafe.unsafe { Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}
