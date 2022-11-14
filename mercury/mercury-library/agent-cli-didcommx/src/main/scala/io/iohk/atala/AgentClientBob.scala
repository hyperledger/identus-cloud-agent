package io.iohk.atala

import zio._
import io.iohk.atala.mercury.Agent
import io.iohk.atala.mercury.AgentService
import io.iohk.atala.mercury.MediaTypes
import io.iohk.atala.mercury.model.UnpackMessage
import io.iohk.atala.mercury.protocol.mailbox.Mailbox.ReadMessage
import org.didcommx.didcomm.message.Attachment.Data.Json
import zhttp.service._

@main def AgentClientBob() = {

  val env = ChannelFactory.auto ++ EventLoopGroup.auto()
  val app = AgentPrograms.senderProgram.provide(env, AgentService.bob)

  Unsafe.unsafe { implicit u => Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}
