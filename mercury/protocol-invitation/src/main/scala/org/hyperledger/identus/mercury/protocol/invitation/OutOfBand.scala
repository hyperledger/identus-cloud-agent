package org.hyperledger.identus.mercury.protocol.invitation

import io.circe.*
import io.circe.parser.*
import org.hyperledger.identus.mercury.protocol.invitation.v2.*

import java.net.{URI, URL}
import java.util as ju

object OutOfBand {

  def parseLink(url: String): Option[String] = parseLink(new URI(url).toURL())
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
