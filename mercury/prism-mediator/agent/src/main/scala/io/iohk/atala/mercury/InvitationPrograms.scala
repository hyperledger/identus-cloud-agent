package io.iohk.atala.mercury

import zio.*
import zhttp.service.Client
import zhttp.http.*
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
    data <- res.bodyAsString
    message = OutOfBand.parseInvitation(data)
    _ <- ZIO.log(message.toString)
  } yield (message)

  def createInvitationV2(): ZIO[Any, Nothing, String] = {
    val invitation = Invitation(
      "https://didcomm.org/out-of-band/2.0/invitation",
      getNewMsgId,
      Agent.Mediator.id.value,
      Body("request-mediate", "RequestMediate", Seq("didcomm/v2", "didcomm/aip2;env=rfc587")),
      None
    )
    val result = invitation.asJson.deepDropNullValues
    ZIO.succeed(java.util.Base64.getUrlEncoder.encodeToString(result.noSpaces.getBytes))
  }

}
