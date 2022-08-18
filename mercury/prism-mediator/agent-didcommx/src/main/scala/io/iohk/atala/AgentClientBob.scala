package io.iohk.atala

import zio.*
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zhttp.http.{Headers, HttpData, Method}
import io.iohk.atala.mercury.Agent
import io.iohk.atala.mercury.AgentService
import io.iohk.atala.mercury.MediaTypes
import io.iohk.atala.mercury.model.UnpackMesage
import io.iohk.atala.mercury.protocol.mailbox.Mailbox.ReadMessage
import org.didcommx.didcomm.message.Attachment.Data.Json
import io.circe.Json._
import io.circe.parser._
import io.circe.JsonObject
@main def AgentClientBob() = {

  def makeReadMessage(from: Agent, mediator: Agent) =
    ReadMessage(from = from.id, to = mediator.id, expires_time = None)

  def toJson(parseToJson: String): JsonObject = {
    val aaa = parse(parseToJson).getOrElse(???)
    aaa.asObject.getOrElse(???)
  }

  val program = for {
    _ <- Console.printLine("\n#### Bob Sending type Readmessages ####")
    messageCreated <- ZIO.succeed(makeReadMessage(Agent.Bob, Agent.Mediator))
    bob <- ZIO.service[AgentService[Agent.Bob.type]]

    // ##########################################
    encryptedMsg <- bob.packEncrypted(messageCreated.asMessage, to = Agent.Mediator.id)
    _ <- Console.printLine("EncryptedMsg: \n" + fromJsonObject(encryptedMsg.asJson).spaces2 + "\n")
    _ <- Console.printLine("Sending bytes ...")
    base64EncodedString = encryptedMsg.base64
    // HTTP
    res <- Client.request(
      url = "http://localhost:8080",
      method = Method.POST,
      headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
      content = HttpData.fromChunk(Chunk.fromArray(base64EncodedString.getBytes)),
      // ssl = ClientSSLOptions.DefaultSSL,
    )
    data <- res.bodyAsString
    _ <- Console.printLine("Receiving the message ..." + data)
    messageReceived <- bob.unpack(data)
    _ <- Console.printLine("Unpacking and decrypting the received message ...")
    _ <- Console.printLine(
      "\n*********************************************************************************************************************************\n"
        + fromJsonObject(toJson(messageReceived.getMessage.toString)).spaces2
        + "\n********************************************************************************************************************************\n"
    )

  } yield ()

  val env = ChannelFactory.auto ++ EventLoopGroup.auto()
  val app = program.provide(env, AgentService.bob)

  Unsafe.unsafe { Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}
