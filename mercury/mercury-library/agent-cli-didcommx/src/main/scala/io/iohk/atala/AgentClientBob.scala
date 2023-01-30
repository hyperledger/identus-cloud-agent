// package io.iohk.atala

// import zio._
// import zio.http.service._
// import io.iohk.atala.mercury._
// import io.iohk.atala.mercury.model.UnpackMessage
// import io.iohk.atala.mercury.protocol.mailbox.Mailbox.ReadMessage
// import org.didcommx.didcomm.message.Attachment.Data.Json

// @main def AgentClientBob() = {

//   val app = AgentPrograms.senderProgram
//     .provide(AgentService.bob, ZioHttpClient.layer)

//   Unsafe.unsafe { implicit u => Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

// }
