package io.iohk.atala

import io.iohk.atala.resolvers.{AliceSecretResolver, BobSecretResolver, MediatorSecretResolver, UniversalDidResolver}
import org.didcommx.didcomm.DIDComm
import org.didcommx.didcomm.message.Attachment.Data.{Companion, Json}
import org.didcommx.didcomm.message.{Attachment, MessageBuilder}
import org.didcommx.didcomm.model.PackEncryptedParams
import org.didcommx.didcomm.model.PackSignedParams
import org.didcommx.didcomm.model.UnpackParams
import org.didcommx.didcomm.protocols.routing.Routing
import org.didcommx.didcomm.secret.SecretResolverInMemory
import org.didcommx.didcomm.utils.JSONUtilsKt.toJson

import java.time.{LocalDateTime, ZoneOffset}
import scala.jdk.CollectionConverters._

import java.util
import java.util.Base64

import zio._

def makeMsg(from: Agent, to: Agent) = {
  val createdTime = LocalDateTime.now().toEpochSecond(ZoneOffset.of("Z"))

  new MessageBuilder(
    java.util.UUID.randomUUID.toString(),
    Map(
      "connectionId" -> "8fb9ea21-d094-4506-86b6-c7c1627d753a",
      "msg" -> "Hello Bob"
    ).asJava,
    "http://atalaprism.io/lets_connect/proposal"
  )
    .from(from.id)
    .to(Seq(to.id).asJava)
    .createdTime(createdTime)
    .expiresTime(createdTime + 1000)
    .build()
}

val program1 = for {
  messageCreated <- ZIO.succeed(makeMsg(Agent.Alice, Agent.Bob))
  alice <- ZIO.service[AgentService[Agent.Alice.type]]
  bob <- ZIO.service[AgentService[Agent.Bob.type]]
  // ##########################################
  signedMsg <- alice.packSigned(messageCreated)
  _ <- Console.printLine("SignedMesagem: " + signedMsg.getPackedMessage())
  aux <- bob.unpack(signedMsg.getPackedMessage())
  _ <- Console.printLine("Bob Check SignedMesagem: " + aux.getMessage())
} yield ()

val program2 = for {
  messageCreated <- ZIO.succeed(makeMsg(Agent.Alice, Agent.Bob))
  alice <- ZIO.service[AgentService[Agent.Alice.type]]
  bob <- ZIO.service[AgentService[Agent.Bob.type]]
  // ##########################################
  _ <- Console.printLine("*" * 120)
  encryptedMsg <- alice.packEncrypted(messageCreated, to = Agent.Bob.id)
  _ <- Console.printLine("EncryptedMsg: " + encryptedMsg.getPackedMessage)
  base64EncodedString = Base64.getUrlEncoder.encodeToString(encryptedMsg.getPackedMessage.getBytes)
  _ <- Console.printLine("Sending bytes ...")
  base64DecodedString = new String(Base64.getUrlDecoder.decode(base64EncodedString))
  mediator <- ZIO.service[AgentService[Agent.Mediator.type]]
  msgInMediator <- mediator.unpack(base64DecodedString)
  _ <- Console.printLine("msgInMediator: " + msgInMediator.getMessage())
  _ <- Console.printLine("Sending bytes to BOB ...")
  msgToBob = msgInMediator.getMessage().getAttachments().get(0).getData().toJSONObject().get("json").toString()
  _ <- Console.printLine("msgToBob: " + msgToBob)
  _ <- Console.printLine("Bob read Messagem ...")
  msgInBob <- bob.unpack(msgToBob)
  _ <- Console.printLine("msgInBob: " + msgInBob.getMessage())
} yield ()

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
