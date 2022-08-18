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

@main def AgentClientBob() = {

  def makeReadMessage(from: Agent, mediator: Agent) =
    ReadMessage(from = from.id, to = mediator.id, expires_time = None)

  val program = for {
    _ <- Console.printLine("\n#### Bob Sending type Readmessages ####")
    messageCreated <- ZIO.succeed(makeReadMessage(Agent.Bob, Agent.Mediator))
    bob <- ZIO.service[AgentService[Agent.Bob.type]]

    // ##########################################
    encryptedMsg <- bob.packEncrypted(messageCreated.asMessage, to = Agent.Mediator.id)
    _ <- Console.printLine("EncryptedMsg: " + encryptedMsg)
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
    // TODO fix this
    dataArray: Seq[String] = data.stripPrefix("[").stripSuffix("]").split(";").toList
    messageReceived: Seq[UIO[UnpackMesage]] <- ZIO.succeed(
      dataArray.map(bob.unpack(_))
    )
    unpackedMsg <- dataArray.map(bob.unpack(_))
    xx <- messageReceived.foreach(_.forEachZIO(ss => Console.printLine("Message Received" + ss.getMessage)))

  } yield ()

  val env = ChannelFactory.auto ++ EventLoopGroup.auto()
  val app = program.provide(env, AgentService.bob)

  Unsafe.unsafe { Runtime.default.unsafe.run(app).getOrThrowFiberFailure() }

}
