package io.iohk.atala.mercury

import zio.*
import io.iohk.atala.mercury.protocol.invitation.*
import io.iohk.atala.mercury.protocol.invitation.v2.*
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation.Body
import io.circe.syntax.*

object InvitationPrograms {

  def getInvitationProgram(url: String) = for {
    _ <- ZIO.log("#### Get Invitation  ####")
    client <- ZIO.service[HttpClient]
    res <- client.get(url = url)
    message = OutOfBand.parseInvitation(res.bodyAsString)
    _ <- ZIO.log(s"*******OutOfBand********${message.toString}")
  } yield (message)

  def createInvitationV2(): ZIO[DidOps & DidAgent, Nothing, String] = {
    for {
      merdiator <- ZIO.service[DidAgent]
      // _ <- ZIO.unit
      invitation = Invitation(
        "https://didcomm.org/out-of-band/2.0/invitation",
        getNewMsgId,
        merdiator.id,
        Body("request-mediate", "RequestMediate", Seq("didcomm/v2", "didcomm/aip2;env=rfc587"))
      )
      _ <- ZIO.log(s"createInvitationV2 from '${merdiator.id}'")
      result = invitation.asJson.deepDropNullValues
    } yield (java.util.Base64.getUrlEncoder.encodeToString(result.noSpaces.getBytes))

  }

}
