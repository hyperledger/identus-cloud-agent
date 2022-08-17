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

  def program(
      base64EncodedString: String
  ): ZIO[DidComm & zio.ZState[io.iohk.atala.mercury.mediator.MyDB], Nothing, Unit] = {
    ZIO.logAnnotate("request-id", java.util.UUID.randomUUID.toString()) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(base64EncodedString)
        mediatorMessage <- unpackBase64(base64EncodedString).map(_.getMessage)
        _ <- ZIO.logInfo("Mediator Message: " + mediatorMessage.toString)
        msg = mediatorMessage.getAttachments().get(0).getData().toJSONObject().get("json").toString()
        to = mediatorMessage.getTo.asScala.toList.head
        _ <- ZIO.log(s"Store Massage for ${to}: " + mediatorMessage.getTo.asScala.toList)
        // db <- ZIO.service[ZState[MyDB]]
        _ <- MyDB.store(DidId(to), msg)
        _ <- ZIO.log("Done")
      } yield ()
    }
  }
}
