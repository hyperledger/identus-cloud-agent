package io.iohk.atala

import zio._

import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zhttp.http.{Method, Headers, HttpData}

import io.iohk.atala.mercury.Agent
import io.iohk.atala.mercury.AgentService
import io.iohk.atala.mercury.MediaTypes
import io.iohk.atala.mercury.protocol.mailbox.Mailbox.ReadMessage

@main def AgentClientBob() = {

  def makeReadMessage(from: Agent, mediator: Agent) =
    ReadMessage(from = from.id, to = mediator.id, expires_time = None)

  val program = for {
    _ <- Console.printLine("\n#### Bob Sending type Readmessages ####")
    messageCreated <- ZIO.succeed(makeReadMessage(Agent.Bob, Agent.Mediator))
    bob <- ZIO.service[AgentService[Agent.Bob.type]]

    // ##########################################
    encryptedMsg <- bob.packEncrypted(messageCreated.asMessage, to = Agent.Mediator.id)
    _ <- Console.printLine("EncryptedMsg: " + encryptedMsg.string)
    _ <- Console.printLine("Sending bytes ...")
    base64EncodedString = encryptedMsg.base64
    _ <- Console.printLine(base64EncodedString)
    // HTTP
    res <- Client.request(
      url = "http://localhost:8080",
      method = Method.POST,
      headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
      content = HttpData.fromChunk(Chunk.fromArray(base64EncodedString.getBytes)),
      // ssl = ClientSSLOptions.DefaultSSL,
    )
    data <- res.bodyAsString
    _ <- Console.printLine(data)
    messageReceived <- bob.unpackBase64(data)
    _ <- Console.printLine("Message Received" + messageReceived.getMessage)

  } yield ()

  val env = ChannelFactory.auto ++ EventLoopGroup.auto()
  val app = program.provide(env, AgentService.bob)

  Unsafe.unsafe { Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}
