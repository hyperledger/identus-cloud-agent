package io.iohk.atala.mercury.protocol.issuecredential

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

import io.iohk.atala.mercury.model.PIURI
import io.iohk.atala.mercury.model._

final case class RequestCredential(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = RequestCredential.`type`,
    body: RequestCredential.Body,
    attachments: Seq[AttachmentDescriptor],
    // extra
    thid: Option[String] = None,
    from: DidId,
    to: DidId,
) {

  def makeMessage: Message = Message(
    id = this.id,
    piuri = this.`type`,
    from = Some(this.from),
    to = Some(this.to),
    thid = this.thid,
    body = this.body.asJson.asObject.get, // TODO get
    attachments = this.attachments,
  )
}
object RequestCredential {

  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/request-credential"

  final case class Body(
      goal_code: Option[String] = None,
      comment: Option[String] = None,
      formats: Seq[CredentialFormat] = Seq.empty[CredentialFormat]
  )

  object Body {
    given Encoder[Body] = deriveEncoder[Body]
    given Decoder[Body] = deriveDecoder[Body]
  }

  def makeRequestCredentialFromOffer(msg: Message): RequestCredential = { // TODO change msg: Message to RequestCredential
    val pc: OfferCredential = OfferCredential.readFromMessage(msg)

    RequestCredential(
      body = RequestCredential.Body(
        goal_code = pc.body.goal_code,
        comment = pc.body.comment,
        formats = pc.body.formats,
      ),
      attachments = pc.attachments,
      thid = Some(msg.id),
      from = msg.to.get, // TODO get
      to = msg.from.get, // TODO get
    )
  }

  def readFromMessage(message: Message): RequestCredential =
    val body = message.body.asJson.as[RequestCredential.Body].toOption.get // TODO get

    RequestCredential(
      id = message.id,
      `type` = message.piuri,
      body = body,
      attachments = message.attachments,
      thid = message.thid,
      from = message.from.get, // TODO get
      to = message.to.get, // TODO get
    )

}
