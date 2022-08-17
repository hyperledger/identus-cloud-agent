package io.iohk.atala

import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio._
import zhttp.http.Method
import zhttp.http.HttpData
import io.iohk.atala.mercury.AgentService
import io.iohk.atala.mercury.Agent

object SimpleClient extends ZIOAppDefault {
  val env = ChannelFactory.auto ++ EventLoopGroup.auto()

  // TEST https://httpbin.org/
  val program = for {
    res <- Client.request(
      url = "https://httpbin.org/post",
      method = Method.POST,
      // headers = Headers.empty,
      content = HttpData.fromChunk(Chunk.fromArray("TestChunk".getBytes)),
      // ssl = ClientSSLOptions.DefaultSSL,
    )
    data <- res.bodyAsString
    _ <- Console.printLine(data)
  } yield ()

  override val run = program.provide(env)

}

@main def AgentClient() = {

  val program = for {
    _ <- Console.printLine("\n#### Program 4 ####")
    messageCreated <- ZIO.succeed(makeMsg(Agent.Alice, Agent.Bob))
    alice <- ZIO.service[AgentService[Agent.Alice.type]]

    // ##########################################
    encryptedMsg <- alice.packEncrypted(messageCreated, to = Agent.Bob.id)
    _ <- Console.printLine("EncryptedMsg: " + encryptedMsg.string)

    forwardMessage = makeForwardMessage(Agent.Alice, Agent.Mediator, Agent.Bob, encryptedMsg).asMessage

    encryptedForwardMessage <- alice.packEncrypted(forwardMessage, to = Agent.Mediator.id)
    _ <- Console.printLine("Sending bytes ...")
    base64EncodedString = encryptedForwardMessage.base64
    _ <- Console.printLine(base64EncodedString)

    // HTTP

    res <- Client.request(
      url = "http://localhost:8080/message",
      method = Method.POST,
      // headers = Headers.empty,
      content = HttpData.fromChunk(Chunk.fromArray(base64EncodedString.getBytes)),
      // ssl = ClientSSLOptions.DefaultSSL,
    )
    data <- res.bodyAsString
    _ <- Console.printLine(data)
  } yield ()

  val env = ChannelFactory.auto ++ EventLoopGroup.auto()
  val app = program.provide(
    env,
    AgentService.alice,
    // AgentService.bob,
    // AgentService.mediator
  )

  Unsafe.unsafe { Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}
