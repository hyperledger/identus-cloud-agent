package io.iohk.atala.mercury.mediator

import zio.*

import scala.jdk.CollectionConverters.*
import io.iohk.atala.mercury.DidComm.*
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.mercury.mediator.MailStorage
import io.iohk.atala.mercury.model.DidId
import io.circe.Json.*
import io.circe.parser.*
import io.circe.JsonObject
import io.iohk.atala.mercury.mediator.MediationState.{Denied, Granted, Requested}

object MediatorProgram {
  val port = 8080

  val startLogo = Console.printLine("""
        |   ███╗   ███╗███████╗██████╗  ██████╗██╗   ██╗██████╗ ██╗   ██╗
        |   ████╗ ████║██╔════╝██╔══██╗██╔════╝██║   ██║██╔══██╗╚██╗ ██╔╝
        |   ██╔████╔██║█████╗  ██████╔╝██║     ██║   ██║██████╔╝ ╚████╔╝
        |   ██║╚██╔╝██║██╔══╝  ██╔══██╗██║     ██║   ██║██╔══██╗  ╚██╔╝
        |   ██║ ╚═╝ ██║███████╗██║  ██║╚██████╗╚██████╔╝██║  ██║   ██║
        |   ╚═╝     ╚═╝╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
        |""".stripMargin) *>
    Console.printLine(
      s"""#####################################################
         |###  Starting the server at http://localhost:$port ###
         |###  Open API docs at http://localhost:$port/docs  ###
         |###  Press ENTER key to exit.                     ###
         |#####################################################""".stripMargin // FIXME But server is not shutting down
    )

  def toJson(parseToJson: String): JsonObject = {
    val aaa = parse(parseToJson).getOrElse(???)
    aaa.asObject.getOrElse(???)
  }

  // val messages = scala.collection mutable.Map[DidId, List[String]]() // TODO must be a service

  // private def messageProcessing(message: org.didcommx.didcomm.message.Message): String =

  def program(
      jsonString: String
  ): ZIO[DidComm & MailStorage & ConnectionStorage, Nothing, String] = {
    ZIO.logAnnotate("request-id", java.util.UUID.randomUUID.toString()) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(jsonString)
        mediatorMessage <- unpack(jsonString).map(_.getMessage)

        ret <- // messageProcessing(mediatorMessage)
          {
            // val recipient = DidId(mediatorMessage.getTo.asScala.toList.head) // FIXME unsafe
            mediatorMessage.getType match {
              case "https://didcomm.org/routing/2.0/forward" =>
                for {
                  _ <- ZIO.logInfo("Mediator Forward Message: " + mediatorMessage.toString)
                  _ <- ZIO.logInfo(
                    "\n*********************************************************************************************************************************\n"
                      + fromJsonObject(toJson(mediatorMessage.toString)).spaces2
                      + "\n********************************************************************************************************************************\n"
                  )
                  msg = mediatorMessage.getAttachments().get(0).getData().toJSONObject().get("json").toString()
                  nextRecipient = DidId(
                    mediatorMessage.getBody.asScala.get("next").map(e => e.asInstanceOf[String]).get
                  )
                  _ <- ZIO.log(s"Store Massage for ${nextRecipient}: " + mediatorMessage.getTo.asScala.toList)
                  // db <- ZIO.service[ZState[MyDB]]
                  _ <- MailStorage.store(nextRecipient, msg)
                  _ <- ZIO.log(s"Stored Message for '$nextRecipient'")
                } yield ("Message Forwarded")
              case "https://atalaprism.io/mercury/mailbox/1.0/ReadMessages" =>
                for {
                  _ <- ZIO.logInfo("Mediator ReadMessages: " + mediatorMessage.toString)
                  senderDID = DidId(mediatorMessage.getFrom())
                  _ <- ZIO.logInfo(s"Mediator ReadMessages get Messages from: $senderDID")
                  seqMsg <- MailStorage.get(senderDID)
                } yield (seqMsg.last)
              case "https://didcomm.org/coordinate-mediation/2.0/mediate-request" =>
                for {
                  _ <- ZIO.logInfo("Mediator ReadMessages: " + mediatorMessage.toString)
                  senderDID = DidId(mediatorMessage.getFrom())
                  _ <- ZIO.logInfo(s"Mediator ReadMessages get Messages from: $senderDID")
                  mayBeConnection <- ConnectionStorage.get(senderDID)
                  _ <- ZIO.logInfo(s"$senderDID state $mayBeConnection")
                  // DO some checks before we grant this logic need more thought
                  grantedOrDenied <- mayBeConnection
                    .map(_ => ZIO.succeed(Denied))
                    .getOrElse(ConnectionStorage.store(senderDID, Granted))
                  _ <- ZIO.logInfo(s"$senderDID state $grantedOrDenied")

                } yield (grantedOrDenied.toString)
              case _ =>
                ZIO.succeed("Unknown Message Type")
            }
          }
      } yield (ret)
    }
  }
}
