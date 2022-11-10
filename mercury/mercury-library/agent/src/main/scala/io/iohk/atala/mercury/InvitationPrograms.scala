package io.iohk.atala.mercury

import zio.*
import zhttp.service.Client
import io.circe.Json.*
import io.circe.parser.*
import io.circe.JsonObject
import io.iohk.atala.mercury.{*, given}
import io.iohk.atala.mercury.model.*
import io.iohk.atala.mercury.protocol.invitation.*
import io.iohk.atala.mercury.protocol.invitation.v2.*
import io.iohk.atala.mercury.protocol.invitation.InvitationCodec.*
import cats.implicits.*
import io.circe.syntax.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.parser.*

object InvitationPrograms {

  def getInvitationProgram(url: String) = for {
    _ <- ZIO.log("#### Get Invitation  ####")
    res <- Client.request(url = url)
    data <- res.body.asString
    message = OutOfBand.parseInvitation(data)
    _ <- ZIO.log(s"*******OutOfBand********${message.toString}")
  } yield (message)

  def createInvitationV2(): ZIO[DidComm, Nothing, String] = {
    for {
      merdiator <- ZIO.service[DidComm]
      // _ <- ZIO.unit
      invitation = Invitation(
        "https://didcomm.org/out-of-band/2.0/invitation",
        getNewMsgId,
        merdiator.myDid,
        Body("request-mediate", "RequestMediate", Seq("didcomm/v2", "didcomm/aip2;env=rfc587")),
        None
      )
      _ <- ZIO.log(s"createInvitationV2 from '${merdiator.myDid}'")
      result = invitation.asJson.deepDropNullValues
    } yield (java.util.Base64.getUrlEncoder.encodeToString(result.noSpaces.getBytes))

  }

}
