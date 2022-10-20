package io.iohk.atala.mercury.model

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.didcommx.didcomm.message.MessageBuilder

import scala.jdk.CollectionConverters._

type PIURI = String //type URI or URL?
case class Message(
    piuri: PIURI,
    from: Option[DidId],
    to: Option[DidId],
    body: Map[String, Any] = Map.empty,
    id: String = java.util.UUID.randomUUID.toString(),
    createdTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("Z")),
    expiresTimePlus: Long = 1000,
    attachments: Seq[Attachment] = Seq.empty, // id -> data  (data is also a json)
    thid: Option[String] = None,
    pthid: Option[String] = None,
    ack: Seq[String] = Seq.empty,
) {}
