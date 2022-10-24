package io.iohk.atala.mercury.mediator

import zio.*

import scala.jdk.CollectionConverters.*
import io.iohk.atala.mercury.DidComm.*
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.mercury.mediator.MailStorage
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.model.Message
import io.circe.Json.*
import io.circe.parser.*
import io.circe.JsonObject
import io.iohk.atala.mercury.mediator.MediationState.{Denied, Granted, Requested}
import io.iohk.atala.mercury.protocol.coordinatemediation.Keylist.Body
import io.iohk.atala.mercury.protocol.coordinatemediation.{MediateDeny, MediateGrant}
import io.iohk.atala.mercury.Agent
object MediatorProgram {
  val port = 8080

  val startLogo =
    for {
      _ <- Console.printLine("""
        |   ███╗   ███╗███████╗██████╗  ██████╗██╗   ██╗██████╗ ██╗   ██╗
        |   ████╗ ████║██╔════╝██╔══██╗██╔════╝██║   ██║██╔══██╗╚██╗ ██╔╝
        |   ██╔████╔██║█████╗  ██████╔╝██║     ██║   ██║██████╔╝ ╚████╔╝
        |   ██║╚██╔╝██║██╔══╝  ██╔══██╗██║     ██║   ██║██╔══██╗  ╚██╔╝
        |   ██║ ╚═╝ ██║███████╗██║  ██║╚██████╗╚██████╔╝██║  ██║   ██║
        |   ╚═╝     ╚═╝╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
        |DID Comm V2 - Mediator agent - Build by Atala (IOHK)
        |""".stripMargin)
      mediator <- ZIO.service[DidComm]
      _ <- Console.printLine(
        s"""
        |#####################################################
        |###  Starting the server at http://localhost:$port
        |###  Open API docs at http://localhost:$port/docs
        |###  ${mediator.myDid.value}
        |###  Press ENTER key to exit.
        |#####################################################""".stripMargin
      )
    } yield ()

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
            mediatorMessage.`type` match {
              case "https://didcomm.org/routing/2.0/forward" =>
                for {
                  _ <- ZIO.logInfo("Mediator Forward Message: " + mediatorMessage.toString)
                  _ <- ZIO.logInfo(
                    "\n*********************************************************************************************************************************\n"
                      + fromJsonObject(toJson(mediatorMessage.toString)).spaces2
                      + "\n********************************************************************************************************************************\n"
                  )
                  msg = mediatorMessage.attachments.map(_.data.toString).head // FIXME Head
                  // msgxx = mediatorMessage.getAttachments().get(0).getData().toJSONObject().get("json").toString() //FIXME REMOVE
                  nextRecipient = DidId(
                    mediatorMessage.body // REMOVE mediatorMessage.getBody.asScala
                      .get("next")
                      .map(e => e.asInstanceOf[String])
                      .get
                  )
                  _ <- ZIO.log(s"Store Massage for ${nextRecipient}: " + mediatorMessage.to)
                  // db <- ZIO.service[ZState[MyDB]]
                  _ <- MailStorage.store(nextRecipient, msg)
                  _ <- ZIO.log(s"Stored Message for '$nextRecipient'")
                } yield ("Message Forwarded")

              case "https://atalaprism.io/mercury/mailbox/1.0/ReadMessages" =>
                for {
                  _ <- ZIO.logInfo("Mediator ReadMessages: " + mediatorMessage.toString)
                  senderDID = mediatorMessage.from.get // FIXME get
                  _ <- ZIO.logInfo(s"Mediator ReadMessages get Messages from: $senderDID")
                  seqMsg <- MailStorage.get(senderDID)
                } yield (seqMsg.last)

              case "https://didcomm.org/coordinate-mediation/2.0/mediate-request" =>
                for {
                  _ <- ZIO.logInfo("Mediator ReadMessages: " + mediatorMessage.toString)
                  senderDID = mediatorMessage.from.get // FIXME get
                  _ <- ZIO.logInfo(s"Mediator ReadMessages get Messages from: $senderDID")
                  mayBeConnection <- ConnectionStorage.get(senderDID)
                  _ <- ZIO.logInfo(s"$senderDID state $mayBeConnection")
                  // DO some checks before we grant this logic need more thought
                  grantedOrDenied <- mayBeConnection
                    .map(_ => ZIO.succeed(Denied))
                    .getOrElse(ConnectionStorage.store(senderDID, Granted))
                  _ <- ZIO.logInfo(s"$senderDID state $grantedOrDenied")
                  messagePrepared <- makeMsg(senderDID, grantedOrDenied)
                  _ <- ZIO.logInfo("Message Prepared: " + messagePrepared.toString)
                  encryptedMsg <- packEncrypted(messagePrepared, to = senderDID)
                  _ <- ZIO.logInfo(
                    "\n*********************************************************************************************************************************\n"
                      + fromJsonObject(encryptedMsg.asJson).spaces2
                      + "\n***************************************************************************************************************************************\n"
                  )

                } yield (fromJsonObject(encryptedMsg.asJson).noSpaces)
              case _ =>
                ZIO.succeed("Unknown Message Type")
            }
          }
      } yield (ret)
    }
  }

  def makeMsg(to: DidId, messageState: MediationState): ZIO[DidComm, Nothing, Message] = for {
    from <- ZIO.service[DidComm].map(_.myDid)
    message = messageState match
      case Granted =>
        val body = MediateGrant.Body(routing_did = from.value)
        val mediateGrant =
          MediateGrant(id = java.util.UUID.randomUUID().toString, `type` = MediateGrant.`type`, body = body)
        Message(
          piuri = mediateGrant.`type`,
          from = Some(from),
          to = Some(to),
          body = Map("routing_did" -> from.value),
        )
      case _ =>
        val mediateDeny =
          MediateDeny(id = java.util.UUID.randomUUID().toString, `type` = MediateDeny.`type`)
        Message(
          piuri = mediateDeny.`type`,
          from = Some(from),
          to = Some(to)
        )
  } yield (message)

}
