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

  def messageProcessing(message: org.didcommx.didcomm.message.Message): Int = {
    val typeOfMessage = message.getType
    val recipient = message.getTo.asScala.toList.head
    val did = DidId(recipient)
    typeOfMessage match {
      case "https://didcomm.org/routing/2.0/forward" => {
        // val msg = message.getAttachments().get(0).getData().toJSONObject().get("json").toString()
        // val msgList: List[String] =
        //   messages.getOrElse(did, List.empty[String]) :+ msg
        // messages += (did -> msgList)
        // "Message Forwarded"
        1
      }
      case "https://atalaprism.io/mercury/mailbox/1.0/ReadMessages" =>
        // messages.getOrElse(did, List.empty[String]).toString()
        2
      case _ => 3 // "Unknown Message Type"
    }
  }

  def program(
      base64EncodedString: String
  ): ZIO[DidComm & zio.ZState[io.iohk.atala.mercury.mediator.MyDB], Nothing, String] = {
    ZIO.logAnnotate("request-id", java.util.UUID.randomUUID.toString()) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(base64EncodedString)
        mediatorMessage <- unpackBase64(base64EncodedString).map(_.getMessage)
        ret <- messageProcessing(mediatorMessage) match {
          case 1 =>
            for {
              _ <- ZIO.logInfo("Mediator Message: " + mediatorMessage.toString)
              msg = mediatorMessage.getAttachments().get(0).getData().toJSONObject().get("json").toString()
              to = mediatorMessage.getTo.asScala.toList.head
              _ <- ZIO.log(s"Store Massage for ${to}: " + mediatorMessage.getTo.asScala.toList)
              // db <- ZIO.service[ZState[MyDB]]
              _ <- MyDB.store(DidId(to), msg)
              _ <- ZIO.log("Done")
            } yield ("Message Forwarded")
          case 2 => ZIO.succeed("BBBBBBB")
          case 3 => ZIO.succeed("CCCC")
        }
      } yield (ret)
    }
  }
}
