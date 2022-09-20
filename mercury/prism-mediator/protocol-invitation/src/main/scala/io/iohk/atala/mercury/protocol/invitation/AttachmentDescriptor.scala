package io.iohk.atala.mercury.protocol.invitation
import java.util.{Base64 => JBase64}
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.syntax._
//
//sealed trait AttachmentData
//
//final case class Base64(base64: String) extends AttachmentData
//
///** @see
//  *   https://github.com/hyperledger/aries-rfcs/tree/main/concepts/0017-attachments
//  * @param `@id`
//  * @param `mime-type`
//  * @param data
//  * @param filename
//  * @param lastmod_time
//  * @param byte_count
//  * @param description
//  */
//final case class AttachmentDescriptor(
//    `@id`: Option[String] = None,
//    `mime-type`: Option[String] = None,
//    data: Base64 = Base64(""),
//    filename: Option[String] = None,
//    lastmod_time: Option[String] = None,
//    byte_count: Option[Int] = None,
//    description: Option[String] = None
//)
//
//object AttachmentDescriptor {
//  def buildAttachment[A: Encoder](
//      id: Option[String] = None,
//      payload: A,
//      mimeType: Option[String] = Some("application/json")
//  ): AttachmentDescriptor = {
//    val encoded = JBase64.getUrlEncoder.encodeToString(payload.asJson.noSpaces.getBytes)
//    AttachmentDescriptor(id, mimeType, Base64(encoded))
//  }
//
//}
