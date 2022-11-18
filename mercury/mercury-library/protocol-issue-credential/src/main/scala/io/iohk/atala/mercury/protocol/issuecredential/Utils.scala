package io.iohk.atala.mercury.protocol.issuecredential

import io.circe.syntax._
import io.circe.parser._

import io.iohk.atala.mercury.model._
import io.circe.Decoder

private[this] trait BodyUtils {
  def formats: Seq[CredentialFormat]
}

private[this] trait ReadAttachmentsUtils {

  def body: BodyUtils
  def attachments: Seq[AttachmentDescriptor]

  /** @return
    *   maping between the credential format name and the credential data in an array of Bytes encoded in base 64
    */
  // protected inline
  lazy val getCredentialFormatAndCredential: Map[String, Array[Byte]] =
    body.formats
      .map { case CredentialFormat(id, formatName) =>
        val maybeAttachament = attachments
          .find(_.id == id)
          .map(_.data match {
            case obj: JwsData  => ??? // TODO
            case obj: Base64   => obj.base64.getBytes()
            case obj: LinkData => ??? // TODO Does this make sens
            case obj: JsonData =>
              java.util.Base64
                .getUrlEncoder()
                .encode(obj.data.asJson.noSpaces.getBytes())
          })
        maybeAttachament.map(formatName -> _)
      }
      .flatten
      .toMap
// eyJhIjoiYSIsImIiOjEsIngiOjIsIm5hbWUiOiJNeU5hbWUiLCJkb2IiOiI_PyJ9
  def getCredential[A](credentialFormatName: String)(using decodeA: Decoder[A]): Option[A] =
    getCredentialFormatAndCredential
      .get(credentialFormatName)
      .map(java.util.Base64.getUrlDecoder().decode(_))
      .map(String(_))
      .map(e => decode[A](e))
      .flatMap(_.toOption)

}
