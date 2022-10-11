package io.iohk.atala.mercury

import scala.util.chaining._
import zio._
import zhttp.service.Client
import zhttp.http._
import io.circe.Json._
import io.circe.syntax._
import io.circe.parser._
import io.circe.JsonObject

import io.iohk.atala.mercury.{_, given}
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.protocol.coordinatemediation._
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation

object CoordinateMediationPrograms {

  def replyToInvitation(from: DidId, invitation: Invitation) = {
    val requestMediation = MediateRequest()
    Message(
      from = from,
      to = DidId(invitation.from),
      id = requestMediation.id,
      piuri = requestMediation.`type`
    )
  }

  def toPrettyJson(parseToJson: String) = {
    parse(parseToJson).getOrElse(???).spaces2
  }

  def senderMediationRequestProgram() = {
    // val mediatorURL = "http://localhost:8000"
    val mediatorURL = "http://localhost:8080"

    def makeMsg(from: Agent, to: DidId) = Message(
      piuri = "http://atalaprism.io/lets_connect/proposal",
      from = from.id,
      to = to,
      body = Map(
        "connectionId" -> "8fb9ea21-d094-4506-86b6-c7c1627d753a",
        "msg" -> "Hello Bob"
      ),
    )

    for {
      _ <- ZIO.log("#### Send Mediation request  ####")
      link <- InvitationPrograms.getInvitationProgram(mediatorURL + "/oob_url")
      planMessage = link.map(to => replyToInvitation(Agent.Charlie.id, to)).get
      invitationFrom = DidId(link.get.from)
      _ <- ZIO.log(s"Invitation from $invitationFrom")

      charlie <- ZIO.service[DidComm]
      encryptedMessage <- charlie.packEncrypted(planMessage, to = invitationFrom)
      _ <- ZIO.log("Sending bytes ...")
      jsonString = encryptedMessage.string
      _ <- ZIO.log(s"\n\n$jsonString\n\n")

      res <- Client.request(
        url = mediatorURL,
        method = Method.POST,
        headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
        content = HttpData.fromChunk(Chunk.fromArray(jsonString.getBytes)),
        // ssl = ClientSSLOptions.DefaultSSL,
      )
      data <- res.bodyAsString
      _ <- ZIO.log(data)

      messageReceived <- charlie.unpack(data)
      _ <- Console.printLine("Unpacking and decrypting the received message ...")
      _ <- Console.printLine("*" * 100)
      _ <- Console.printLine(toPrettyJson(messageReceived.getMessage.toString))
      _ <- Console.printLine("*" * 100)
      ret = parse(messageReceived.getMessage.toString)
        .getOrElse(???)
        // .flatMap()
        .pipe { json =>
          json.as[MediateGrant] match {
            case Right(mediateGrant) => Right(mediateGrant)
            case Left(_) =>
              json.as[MediateDeny] match {
                case Right(mediateDeny) => Left(mediateDeny)
                case Left(_)            => ???
              }
          }
        }
      _ <- Console.printLine(ret)
      _ <- Console.printLine("*" * 100)
    } yield (ret)
  }

}
