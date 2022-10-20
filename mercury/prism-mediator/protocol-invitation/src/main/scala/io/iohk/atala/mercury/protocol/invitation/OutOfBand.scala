package io.iohk.atala.mercury.protocol.invitation

import java.net.URL
import java.{util => ju}
import io.iohk.atala.mercury.protocol.invitation.v2._
import io.iohk.atala.mercury.protocol.invitation.InvitationCodec._
import io.circe._
import io.circe.parser._
import io.iohk.atala.mercury

object OutOfBand {

  def parseLink(url: String): Option[String] = parseLink(new URL(url))
  def parseLink(url: URL): Option[String] = (url.getQuery() match {
    case str if str.startsWith("_oob=") => Some(str.drop(5))
    case _                              => None
  }).map { e =>
    val decoder = ju.Base64.getUrlDecoder()
    String(decoder.decode(e))
  }

  def parseInvitation(url: String): Either[io.circe.Error | RuntimeException, Invitation] =
    parseLink(url) match {
      case Some(e) => parse(e).flatMap(_.as[Invitation])
      case None    => Left(new RuntimeException("Expeting a url!"))
    }

}
