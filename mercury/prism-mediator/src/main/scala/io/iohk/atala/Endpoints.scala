package io.iohk.atala

import io.circe.generic.auto._
import io.iohk.atala.Mediator.{ConnectionId, Message, httpErrors}
import io.iohk.atala.outofband.{CreateInvitation, CreateInvitationResponse}
import sttp.apispec.openapi.circe.yaml._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.ztapir._
import sttp.tapir.{PublicEndpoint, endpoint, query}
import zio.ZIO

import scala.collection.mutable

object Endpoints {

  val messages = mutable.Map[ConnectionId, String]()

  val retrieveMessages: PublicEndpoint[ConnectionId, Unit, List[String], Any] = endpoint.get
    .tag("mediator")
    .summary("Retrieve Messages for connectionId")
    .in("mediator" / "messages")
    .in(query[ConnectionId]("connectionId"))
    .out(jsonBody[List[String]])

  val retrieveMessagesServerEndpoint: ZServerEndpoint[Any, Any] =
    retrieveMessages.serverLogicSuccess(id => ZIO.succeed(List(messages.getOrElse(ConnectionId(id.connectionId), ""))))

  val registerMediator: PublicEndpoint[ConnectionId, ErrorInfo, Unit, Any] =
    endpoint.post
      .tag("mediator")
      .summary("Registers the agent with the router.")
      .in("mediator" / "register")
      .in(jsonBody[ConnectionId])
      .errorOut(httpErrors)
  val registerMediatorServerEndpoint: ZServerEndpoint[Any, Any] = registerMediator.serverLogicSuccess(_ => ZIO.succeed(()))

  val sendMessage: PublicEndpoint[Message, ErrorInfo, Unit, Any] =
    endpoint.post
      .tag("mediator")
      .summary("Mediator service endpoint for sending message")
      .in("mediator" / "send" / "message")
      .in(jsonBody[Message])
      .errorOut(httpErrors)

  val sendMessageServerEndpoint: ZServerEndpoint[Any, Any] = sendMessage.serverLogicSuccess { message =>
    messages += ConnectionId(message.connectionId) -> message.msg
    ZIO.succeed(())
  }

  val createInvitation: PublicEndpoint[CreateInvitation, ErrorInfo, CreateInvitationResponse, Any] =
    endpoint.post
      .tag("out-of-band")
      .summary("Create a new DIDCommV2 out of band invitation.")
      .in("outofband" / "create-invitation")
      .in(jsonBody[CreateInvitation])
      .errorOut(httpErrors)
      .out(jsonBody[CreateInvitationResponse])
      .description("Create Invitation Response")

  val createInvitationServerEndpoint: ZServerEndpoint[Any, Any] =
    createInvitation.serverLogicSuccess(invitation => ZIO.succeed(CreateInvitationResponse(invitation.goal, invitation.goal_code)))

  val all: List[ZServerEndpoint[Any, Any]] =
    List(createInvitationServerEndpoint, retrieveMessagesServerEndpoint, registerMediatorServerEndpoint, sendMessageServerEndpoint)
  val docs =
    OpenAPIDocsInterpreter().toOpenAPI(
      List(createInvitation, retrieveMessages, registerMediator, sendMessage),
      "Atala Prism Mediator",
      "0.1.0"
    )
  val yaml = docs.toYaml
  println(yaml)
}
