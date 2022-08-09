package io.iohk.atala.model

import io.iohk.atala.Agent
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.didcommx.didcomm.message.MessageBuilder

import scala.jdk.CollectionConverters._

type PIURI = String //type URI or URL?
case class Message(
    from: Agent,
    to: Agent,
    body: Map[String, Any],
    id: String = java.util.UUID.randomUUID.toString(),
    piuri: PIURI = "http://atalaprism.io/lets_connect/proposal",
    createdTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("Z")),
    expiresTimePlus: Long = 1000
) {}

// TODO move you another module
given Conversion[Message, org.didcommx.didcomm.message.Message] with {
  def apply(msg: Message): org.didcommx.didcomm.message.Message =
    new MessageBuilder(msg.id, msg.body.asJava, msg.piuri)
      .from(msg.from.id)
      .to(Seq(msg.to.id).asJava)
      .createdTime(msg.createdTime)
      .expiresTime(msg.createdTime + msg.expiresTimePlus)
      .build()
}
