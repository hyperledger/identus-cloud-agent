package io.iohk.atala

import zio._
import zhttp.service.Client
import zhttp.http._

import io.circe._
import io.circe.Json._
import io.circe.parser._
import io.circe.JsonObject
import io.iohk.atala.mercury.{_, given}
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.protocol.mailbox.Mailbox.ReadMessage
import io.iohk.atala.mercury.protocol.routing._

def makeMsg(from: Agent, to: Agent) = Message(
  piuri = "http://atalaprism.io/lets_connect/proposal",
  from = Some(from.id),
  to = Some(to.id),
  body = JsonObject.fromIterable(
    Seq(
      "connectionId" -> Json.fromString("8fb9ea21-d094-4506-86b6-c7c1627d753a"),
      "msg" -> Json.fromString("Hello Bob")
    )
  )
)

def makeForwardMessage(from: Agent, mediator: Agent, to: Agent, msg: EncryptedMessage) =
  ForwardMessage(
    from = from.id,
    to = mediator.id,
    expires_time = None,
    body = ForwardBody(next = to.id), // TODO check msg header
    attachments = Seq(AttachmentDescriptor.buildAttachment(payload = msg.asJson)),
  )

object AgentPrograms {

  def toPrettyJson(parseToJson: String) = {
    parse(parseToJson).getOrElse(???).spaces2
  }

  def makeReadMessage(from: Agent, mediator: Agent) =
    ReadMessage(from = from.id, to = mediator.id, expires_time = None)

  val senderProgram = for {
    _ <- Console.printLine("\n#### Bob Sending type Readmessages ####")
    messageCreated <- ZIO.succeed(makeReadMessage(Agent.Bob, Agent.Mediator))
    bob <- ZIO.service[DidComm] // AgentService[Agent.Bob.type]]

    // ##########################################
    encryptedMsg <- bob.packEncrypted(messageCreated.asMessage, to = Agent.Mediator.id)
    _ <- Console.printLine("EncryptedMsg: \n" + fromJsonObject(encryptedMsg.asJson).spaces2 + "\n")
    _ <- Console.printLine("Sending bytes ...")
    jsonString = encryptedMsg.string
    // HTTP
    res <- Client.request(
      url = "http://localhost:8080",
      method = Method.POST,
      headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
      content = HttpData.fromChunk(Chunk.fromArray(jsonString.getBytes)),
      // ssl = ClientSSLOptions.DefaultSSL,
    )
    data <- res.bodyAsString
    _ <- Console.printLine("Receiving the message ..." + data)
    messageReceived <- bob.unpack(data)
    _ <- Console.printLine("Unpacking and decrypting the received message ...")
    _ <- Console.printLine(
      "\n*********************************************************************************************************************************\n"
        + toPrettyJson(messageReceived.getMessage.toString)
        + "\n********************************************************************************************************************************\n"
    )

  } yield ()

  val pickupMessageProgram = for {
    _ <- Console.printLine("\n#### Program 4 ####")
    messageCreated <- ZIO.succeed(makeMsg(Agent.Alice, Agent.Bob))
    alice <- ZIO.service[DidComm] // AgentService[Agent.Alice.type]]
    _ <- Console.printLine("Send Message")
    _ <- Console.printLine(
      "\n*********************************************************************************************************************************\n"
        + messageCreated // toPrettyJson(msg.toString)
        + "\n********************************************************************************************************************************\n"
    )
    // ##########################################
    encryptedMsg <- alice.packEncrypted(messageCreated, to = Agent.Bob.id)
    _ <- Console.printLine("EncryptedMsg: " + encryptedMsg.asJson)
    _ <- Console.printLine(
      "\n*********************************************************************************************************************************\n"
        + fromJsonObject(encryptedMsg.asJson).spaces2
        + "\n********************************************************************************************************************************\n"
    )
    forwardMessage = makeForwardMessage(Agent.Alice, Agent.Mediator, Agent.Bob, encryptedMsg).asMessage

    encryptedForwardMessage <- alice.packEncrypted(forwardMessage, to = Agent.Mediator.id)
    _ <- Console.printLine("Sending bytes ...")
    jsonString = encryptedForwardMessage.string
    _ <- Console.printLine(jsonString)

    // HTTP

    alice <- ZIO.service[DidComm]
    res <- Client.request(
      url = "http://localhost:8080",
      method = Method.POST,
      headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
      content = HttpData.fromChunk(Chunk.fromArray(jsonString.getBytes)),
      // ssl = ClientSSLOptions.DefaultSSL,
    )
    data <- res.bodyAsString
    _ <- Console.printLine(data)
  } yield ()

}
