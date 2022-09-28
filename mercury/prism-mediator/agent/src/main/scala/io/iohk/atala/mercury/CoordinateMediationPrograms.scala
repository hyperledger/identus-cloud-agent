package io.iohk.atala.mercury

import zio._
import zhttp.service.Client
import zhttp.http._
import io.circe.Json._
import io.circe.parser._
import io.circe.JsonObject

import io.iohk.atala.mercury.{_, given}
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.protocol.coordinatemediation._
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation

object CoordinateMediationPrograms {

  def replyToInvitation(invitation: Invitation) = {
    val requestMediation = MediateRequest()
    Message(
      from = DidId(""),
      to = DidId(invitation.from),
      id = requestMediation.id,
      piuri = requestMediation.`type`
    )
  }

  def senderMediationRequestProgram() = {
    val mediatorURL = "http://localhost:8000"

    for {
      _ <- ZIO.log("#### Send Mediation request  ####")
      link <- InvitationPrograms.getInvitationProgram(mediatorURL + "/oob_url")
      planMessage = link.map(replyToInvitation(_)).get
      _ <- ZIO.log(planMessage.toString())
      invitationFrom = DidId(link.get.from)
      _ <- ZIO.log(s"Invitation from $invitationFrom")

      charlie <- ZIO.service[DidComm]
      encryptedMessage <- charlie.packEncrypted(planMessage, to = invitationFrom)
      _ <- ZIO.log("Sending bytes ...")
      // jsonString = encryptedMessage.string
      // _ <- ZIO.log(jsonString)

      // res <- Client.request(
      //   url = mediatorURL,
      //   method = Method.POST,
      //   headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
      //   content = HttpData.fromChunk(Chunk.fromArray(jsonString.getBytes)),
      //   // ssl = ClientSSLOptions.DefaultSSL,
      // )

    } yield ()
  }

  // data <- res.bodyAsString
  // _ <- Console.printLine(data)

}
