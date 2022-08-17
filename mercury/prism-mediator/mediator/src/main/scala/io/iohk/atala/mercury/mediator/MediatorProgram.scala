package io.iohk.atala.mercury.mediator

import zio._
import scala.jdk.CollectionConverters.*

import io.iohk.atala.mercury.DidComm._
import io.iohk.atala.mercury.DidComm

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

  def program(base64EncodedString: String): ZIO[DidComm, Nothing, Unit] = {
    ZIO.logAnnotate("request-id", java.util.UUID.randomUUID.toString()) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(base64EncodedString)
        mediatorMessage <- unpackBase64(base64EncodedString).map(_.getMessage)
        _ <- ZIO.logInfo("Mediator Message: " + mediatorMessage.toString)
        msg = mediatorMessage.getAttachments().get(0).getData().toJSONObject().get("json").toString()
        to = mediatorMessage.getTo.asScala.toList.head
        _ <- ZIO.log(s"Store Massage for ${to}: " + mediatorMessage.getTo.asScala.toList)
        // _ <- ZIO.succeed {
        //   val msgList: List[String] =
        //     messages.getOrElse(DidId(to), List.empty[String]) :+ msg
        //     messages += (DidId(to) -> msgList)
        // }
        _ <- ZIO.log("Done")
      } yield ()
    }
  }
}
