package io.iohk.atala.mercury.mediator

import zio._
import scala.jdk.CollectionConverters.*

import io.iohk.atala.mercury.DidComm._
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.mercury.mediator.MyDB
import io.iohk.atala.mercury.model.DidId

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

  // val messages = scala.collection mutable.Map[DidId, List[String]]() // TODO must be a service

  // private def messageProcessing(message: org.didcommx.didcomm.message.Message): String =

  def program(
      base64EncodedString: String
  ): ZIO[DidComm /* & ZDB*/, Nothing, String] = {
    ZIO.logAnnotate("request-id", java.util.UUID.randomUUID.toString()) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(base64EncodedString)
        mediatorMessage <- unpackBase64(base64EncodedString).map(_.getMessage)
        ret <- // messageProcessing(mediatorMessage)
          {
            // val recipient = DidId(mediatorMessage.getTo.asScala.toList.head) // FIXME unsafe
            mediatorMessage.getType match {
              case "https://didcomm.org/routing/2.0/forward" =>
                for {
                  _ <- ZIO.logInfo("Mediator Forward Message: " + mediatorMessage.toString)
                  msg = mediatorMessage.getAttachments().get(0).getData().toJSONObject().get("json").toString()
                  nextRecipient = DidId(
                    mediatorMessage.getBody.asScala.get("next").map(e => e.asInstanceOf[String]).get
                  )
                  _ <- ZIO.log(s"Store Massage for ${nextRecipient}: " + mediatorMessage.getTo.asScala.toList)
                  // db <- ZIO.service[ZState[MyDB]]
                  _ <- MyDB.store(nextRecipient, msg)
                  _ <- ZIO.log(s"Stored Message for '$nextRecipient'")
                } yield ("Message Forwarded")
              case "https://atalaprism.io/mercury/mailbox/1.0/ReadMessages" =>
                for {
                  _ <- ZIO.logInfo("Mediator ReadMessages: " + mediatorMessage.toString)
                  senderDID = DidId(mediatorMessage.getFrom())
                  _ <- ZIO.logInfo(s"Mediator ReadMessages get Messagems from: $senderDID")
                  seqMsg <- MyDB.get(senderDID)
                } yield (seqMsg.mkString("[", "; ", "]"))
              case _ =>
                ZIO.succeed("Unknown Message Type")
            }
          }
      } yield (ret)
    }
  }
}
