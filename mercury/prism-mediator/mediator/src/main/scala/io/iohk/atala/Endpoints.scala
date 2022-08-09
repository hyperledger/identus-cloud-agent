package io.iohk.atala

import io.circe.generic.auto.*
import io.iohk.atala.Mediator.{ConnectionId, Message, httpErrors}
import io.iohk.atala.outofband.{CreateInvitation, CreateInvitationResponse}
import sttp.apispec.openapi.circe.yaml.*
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import sttp.tapir.{PublicEndpoint, endpoint, query}
import zio.{Console, RIO, ZIO}

import java.io.IOException
import scala.collection.mutable

object Endpoints {

  val messages = mutable.Map[ConnectionId, List[String]]()

  val retrieveMessages: PublicEndpoint[String, Unit, List[String], Any] = endpoint.get
    .tag("mediator")
    .summary("Retrieve Messages for connectionId")
    .in("mediator" / "messages")
    .in(query[String]("connectionId"))
    .out(jsonBody[List[String]])

  val retrieveMessagesServerEndpoint: ZServerEndpoint[Any, Any] =
    retrieveMessages.serverLogicSuccess(id => ZIO.succeed(messages.getOrElse(ConnectionId(id), List.empty[String])))

  val registerMediator: PublicEndpoint[ConnectionId, ErrorInfo, Unit, Any] =
    endpoint.post
      .tag("mediator")
      .summary("Registers the agent with the router.")
      .in("mediator" / "register")
      .in(jsonBody[ConnectionId])
      .errorOut(httpErrors)
  val registerMediatorServerEndpoint: ZServerEndpoint[Any, Any] =
    registerMediator.serverLogicSuccess(_ => ZIO.succeed(()))

  val sendMessage: PublicEndpoint[Message, ErrorInfo, Unit, Any] =
    endpoint.post
      .tag("mediator")
      .summary("Mediator service endpoint for sending message")
      .in("mediator" / "message")
      .in(jsonBody[Message])
      .errorOut(httpErrors)

  val sendMessageServerEndpoint: ZServerEndpoint[Any, Any] = {
    sendMessage.serverLogicSuccess { (message: Message) =>
      for {
        mediator <- ZIO.service[AgentService[Agent.Mediator.type]]
        unPackMsg <- mediator.unpack(message.msg)
        _ <- Console.printLine("SignedMessage: " + unPackMsg.getMessage)
        sss <- ZIO.succeed {
          val msgList: List[String] =
            messages.getOrElse(ConnectionId(message.connectionId), List.empty[String]) :+ unPackMsg.getMessage.toString
          messages += (ConnectionId(message.connectionId) -> msgList)
        }
      } yield ()

    //  ZIO.succeed(())
    }
  }

  val createInvitation: PublicEndpoint[CreateInvitation, ErrorInfo, CreateInvitationResponse, Any] = {
    endpoint.post
      .tag("out-of-band")
      .summary("Create a new DIDCommV2 out of band invitation.")
      .in("outofband" / "create-invitation")
      .in(jsonBody[CreateInvitation])
      .out(jsonBody[CreateInvitationResponse])
      .errorOut(httpErrors)
      .description("Create Invitation Response")
  }

  val createInvitationServerEndpoint: ZServerEndpoint[Any, Any] =
    createInvitation.serverLogicSuccess(invitation =>
      ZIO.succeed(CreateInvitationResponse(invitation.goal, invitation.goal_code))
    )

  val all: List[ZServerEndpoint[Any, Any]] =
    List(
      createInvitationServerEndpoint,
      retrieveMessagesServerEndpoint,
      registerMediatorServerEndpoint,
      sendMessageServerEndpoint
    )

  val docs =
    OpenAPIDocsInterpreter().toOpenAPI(
      List(createInvitation, retrieveMessages, registerMediator, sendMessage),
      "Atala Prism Mediator",
      "0.1.0"
    )

  val yaml = docs.toYaml

  println(yaml)
}
