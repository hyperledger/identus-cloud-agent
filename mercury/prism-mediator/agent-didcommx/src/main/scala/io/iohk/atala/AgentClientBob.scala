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

@main def AgentClientBob() = {

  val env = ChannelFactory.auto ++ EventLoopGroup.auto()
  val app = AgentPrograms.senderProgram.provide(env, AgentService.bob)

  Unsafe.unsafe { implicit u => Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}
