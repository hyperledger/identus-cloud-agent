package io.iohk.atala.mercury.model

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.didcommx.didcomm.message.MessageBuilder

import scala.jdk.CollectionConverters._

type PIURI = String //type URI or URL?
case class Message(
    from: DidId,
    to: DidId,
    body: Map[String, Any],
    id: String = java.util.UUID.randomUUID.toString(),
    piuri: PIURI = "http://atalaprism.io/lets_connect/proposal",
    createdTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("Z")),
    expiresTimePlus: Long = 1000,
    attachments: Seq[Attachment] = Seq.empty, // id -> data  (data is also a json)
    thid: Option[String] = None,
    pthid: Option[String] = None,
    ack: Seq[String] = Seq.empty,
) {}
