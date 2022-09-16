package io.iohk.atala.mercury.protocol.invitation

import java.net.URL
import java.{util => ju}
import io.iohk.atala.mercury.protocol.invitation.v2._
import io.iohk.atala.mercury.protocol.invitation.InvitationCodec._
import io.circe._
import io.circe.parser._

object OutOfBand {

  def parseLink(url: String): Option[String] = parseLink(new URL(url))
  def parseLink(url: URL): Option[String] = (url.getQuery() match {
    case str if str.startsWith("_oob=") => Some(str.drop(5))
    case _                              => None
  }).map { e =>
    val decoder = ju.Base64.getUrlDecoder()
    String(decoder.decode(e))
  }

  def parseInvitation(url: String): Option[Invitation] = {
    parseLink(url).map(e => parse(e).getOrElse(???).as[Invitation].getOrElse(???))
  }

  // {"type":"https://didcomm.org/out-of-band/2.0/invitation",
  //   "id":"421dbbc8-57ca-4341-aa3a-f5b4215c568f",
  //   "from":"did:peer:2.Ez6LSmLmWmTvwjgLSuUaEQHdHSFWPwyibgzomWjFmnC6FhLnU.Vz6MktNgLh4N1u9KNhDiqe8KZ8bsLzLcqsifoNiUtBoSs9jxf.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwOi8vMTI3LjAuMC4xOjgwMDAiLCJhIjpbImRpZGNvbW0vdjIiXX0",
  //   "body":{
  //     "goal_code":"request-mediate",
  //     "goal":"RequestMediate",
  //     "accept":["didcomm/v2","didcomm/aip2;env=rfc587"]
  //   }
  // }

}
