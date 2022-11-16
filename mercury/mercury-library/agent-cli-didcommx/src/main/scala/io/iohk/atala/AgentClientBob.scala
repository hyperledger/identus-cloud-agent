package io.iohk.atala

import zio._
import io.iohk.atala.mercury._
import io.iohk.atala.mercury.model.UnpackMessage
import io.iohk.atala.mercury.protocol.mailbox.Mailbox.ReadMessage
import org.didcommx.didcomm.message.Attachment.Data.Json
import zhttp.service._

@main def AgentClientBob() = {

  val app = AgentPrograms.senderProgram
    .provide(AgentService.bob, HttpClientZhttp.layer)

  Unsafe.unsafe { implicit u => Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}
