package io.iohk.atala

import java.time.{LocalDateTime, ZoneOffset}
import scala.jdk.CollectionConverters._

import java.util
import java.util.Base64

import zio._

import io.iohk.atala.model.Message
import io.iohk.atala.resolvers.{AliceSecretResolver, BobSecretResolver, MediatorSecretResolver, UniversalDidResolver}

def makeMsg(from: Agent, to: Agent) = Message(
  from,
  to,
  Map(
    "connectionId" -> "8fb9ea21-d094-4506-86b6-c7c1627d753a",
    "msg" -> "Hello Bob"
  ),
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

  Unsafe.unsafe { Runtime.default.unsafe.run(app1).getOrThrowFiberFailure() }
  Unsafe.unsafe { Runtime.default.unsafe.run(app2).getOrThrowFiberFailure() }

}
