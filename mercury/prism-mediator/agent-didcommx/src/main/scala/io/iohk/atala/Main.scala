package io.iohk.atala

import java.time.{LocalDateTime, ZoneOffset}
import scala.jdk.CollectionConverters._

import java.util
import java.util.Base64

import zio._

import io.iohk.atala.resolvers.{AliceSecretResolver, BobSecretResolver, MediatorSecretResolver, UniversalDidResolver}
import io.iohk.atala.mercury.model.Message
import io.iohk.atala.mercury.Agent
import io.iohk.atala.mercury.AgentService
import io.iohk.atala.mercury.protocol.routing._
import io.iohk.atala.mercury.model.Attachment
import io.iohk.atala.mercury.model.EncryptedMessage

def makeMsg(from: Agent, to: Agent) = Message(
  from.id,
  to.id,
  Map(
    "connectionId" -> "8fb9ea21-d094-4506-86b6-c7c1627d753a",
    "msg" -> "Hello Bob"
  ),
)

def makeForwardMessage(from: Agent, mediator: Agent, to: Agent, msg: EncryptedMessage) =
  ForwardMessage(
    from = from.id,
    to = mediator.id,
    expires_time = None,
    body = ForwardBody(next = to.id), // TODO check msg header
    attachments = Seq(Attachment(data = msg.asJson)),
  )

val program1 = for {
  messageCreated <- ZIO.succeed(makeMsg(Agent.Alice, Agent.Bob))
  alice <- ZIO.service[AgentService[Agent.Alice.type]]
  bob <- ZIO.service[AgentService[Agent.Bob.type]]
  // ##########################################
  signedMsg <- alice.packSigned(messageCreated)
  _ <- Console.printLine("SignedMesagem: " + signedMsg.string)
  aux <- bob.unpackBase64(signedMsg.base64)
  _ <- Console.printLine("Bob Check SignedMesagem: " + aux.getMessage)
} yield ()

val program2 = for {
  messageCreated <- ZIO.succeed(makeMsg(Agent.Alice, Agent.Bob))
  alice <- ZIO.service[AgentService[Agent.Alice.type]]
  bob <- ZIO.service[AgentService[Agent.Bob.type]]
  // ##########################################
  _ <- Console.printLine("*" * 120)
  encryptedMsg <- alice.packEncrypted(messageCreated, to = Agent.Bob.id)
  _ <- Console.printLine("EncryptedMsg: " + encryptedMsg.string)
  base64EncodedString = encryptedMsg.base64
  _ <- Console.printLine("Sending bytes ...")
  // base64DecodedString = new String(Base64.getUrlDecoder.decode(base64EncodedString))
  mediator <- ZIO.service[AgentService[Agent.Mediator.type]]
  msgInMediator <- mediator.unpackBase64(base64EncodedString)
  _ <- Console.printLine("msgInMediator: " + msgInMediator.getMessage)
  _ <- Console.printLine("Sending bytes to BOB ...")
  msgToBob = msgInMediator.getMessage.getAttachments().get(0).getData().toJSONObject().get("json").toString()
  _ <- Console.printLine("msgToBob: " + msgToBob)
  _ <- Console.printLine("Bob read Messagem ...")
  msgInBob <- bob.unpack(msgToBob)
  _ <- Console.printLine("msgInBob: " + msgInBob.getMessage)
} yield ()

val program3 = for {
  _ <- Console.printLine("\n#### Program 3 ####")
  messageCreated <- ZIO.succeed(makeMsg(Agent.Alice, Agent.Bob))
  alice <- ZIO.service[AgentService[Agent.Alice.type]]
  bob <- ZIO.service[AgentService[Agent.Bob.type]]
  // ##########################################
  encryptedMsg <- alice.packEncrypted(messageCreated, to = Agent.Bob.id)
  _ <- Console.printLine("EncryptedMsg: " + encryptedMsg.string)
  msgInBob <- bob.unpack(encryptedMsg.string)
  _ <- Console.printLine("msgInBob: " + msgInBob.getMessage)
} yield ()

val program4 = for {
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

  // _ <- Console.printLine("Sending bytes ...")
  base64DecodedString = new String(Base64.getUrlDecoder.decode(base64EncodedString))
  mediator <- ZIO.service[AgentService[Agent.Mediator.type]]
  msgInMediator <- mediator.unpackBase64(base64EncodedString)
  _ <- Console.printLine("msgInMediator: ")
  _ <- Console.printLine(msgInMediator.getMessage)
  _ <- Console.printLine("Sending bytes to BOB ...")
  msgToBob = msgInMediator.getMessage.getAttachments().get(0).getData().toJSONObject().get("json").toString()
  _ <- Console.printLine("msgToBob: " + msgToBob)
  _ <- Console.printLine("Bob read Messagem ...")

  // bob <- ZIO.service[AgentService[Agent.Bob.type]]
  // msgInBob <- bob.unpack(msgToBob)
  // _ <- Console.printLine("msgInBob: " + msgInBob.getMessage)
} yield ()

// TODO Make tests and remove this main
@main def didCommPlay() = {

  val app1 = program1.provide(
    AgentService.alice,
    AgentService.bob
  )

  val app2 = program2.provide(
    AgentService.alice,
    AgentService.bob,
    AgentService.mediator
  )

  val app3 = program3.provide(
    AgentService.alice,
    AgentService.bob
  )

  val app4 = program4.provide(
    AgentService.alice,
    AgentService.bob,
    AgentService.mediator
  )

  // Unsafe.unsafe { Runtime.default.unsafe.run(app1).getOrThrowFiberFailure() }
  // Unsafe.unsafe { Runtime.default.unsafe.run(app2).getOrThrowFiberFailure() }
  // Unsafe.unsafe { Runtime.default.unsafe.run(app3).getOrThrowFiberFailure() }
  Unsafe.unsafe { Runtime.default.unsafe.run(app4).getOrThrowFiberFailure() }

}
