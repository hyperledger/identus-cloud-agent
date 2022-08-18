package io.iohk.atala

import zio._

import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zhttp.http.{Method, Headers, HttpData}

import io.iohk.atala.mercury.Agent
import io.iohk.atala.mercury.AgentService
import io.iohk.atala.mercury.MediaTypes
import io.circe.Printer
import io.circe.syntax._

@main def AgentClientAlice() = {
  val printer = Printer.spaces4
  val program = for {
    _ <- Console.printLine("\n#### Program 4 ####")
    messageCreated <- ZIO.succeed(makeMsg(Agent.Alice, Agent.Bob))
    alice <- ZIO.service[AgentService[Agent.Alice.type]]

    // ##########################################
    encryptedMsg <- alice.packEncrypted(messageCreated, to = Agent.Bob.id)
    _ <- Console.printLine("EncryptedMsg: " + encryptedMsg.asJson)

    forwardMessage = makeForwardMessage(Agent.Alice, Agent.Mediator, Agent.Bob, encryptedMsg).asMessage

    encryptedForwardMessage <- alice.packEncrypted(forwardMessage, to = Agent.Mediator.id)
    _ <- Console.printLine("Sending bytes ...")
    base64EncodedString = encryptedForwardMessage.base64
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
  } yield ()

  val env = ChannelFactory.auto ++ EventLoopGroup.auto()
  val app = program.provide(env, AgentService.alice)

  Unsafe.unsafe { Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}
