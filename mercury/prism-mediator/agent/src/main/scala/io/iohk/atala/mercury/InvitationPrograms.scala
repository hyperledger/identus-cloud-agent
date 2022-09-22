package io.iohk.atala.mercury

import zio._
import zhttp.service.Client
import zhttp.http._
import io.circe.Json._
import io.circe.parser._
import io.circe.JsonObject

import io.iohk.atala.mercury.{_, given}
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.protocol.invitation._

object InvitationPrograms {

  def getInvitationProgram(url: String) = for {
    _ <- ZIO.log("#### Get Invitation  ####")
    res <- Client.request(url = url)
    data <- res.bodyAsString
    message = OutOfBand.parseInvitation(data)
    _ <- ZIO.log(message.toString)
  } yield (message)
}
