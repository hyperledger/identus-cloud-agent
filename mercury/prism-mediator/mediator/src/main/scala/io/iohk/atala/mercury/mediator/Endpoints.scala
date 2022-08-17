package io.iohk.atala.mercury.mediator

import io.circe.generic.auto.*
import sttp.apispec.openapi.circe.yaml.*
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import sttp.tapir.{PublicEndpoint, endpoint, query}
import zio.*

import java.io.IOException
import scala.collection.mutable
import io.iohk.atala.mercury.mediator.*
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.{Agent, AgentService, DidComm}
import io.iohk.atala.mercury.protocol.invitation.*

import scala.jdk.CollectionConverters.*
import java.util.Base64
// import io.iohk.atala.mercury.DidComm._ // For the dsl

object Endpoints {

  // val messages = mutable.Map[ConnectionId, List[String]]()
  val messages = mutable.Map[DidId, List[String]]()

  val retrieveMessages: PublicEndpoint[String, Unit, List[String], Any] = endpoint.get
    .tag("mediator")
    .summary("Retrieve Messages")
    .in("messages")
    .in(path[String]("connectionId"))
    .out(jsonBody[List[String]])

  val retrieveMessagesServerEndpoint: ZServerEndpoint[DidComm, Any] =
    retrieveMessages.serverLogicSuccess(id => ZIO.succeed(messages.getOrElse(DidId(id), List.empty[String])))

  val registerMediator: PublicEndpoint[ConnectionId, ErrorInfo, Unit, Any] =
    endpoint.post
      .tag("mediator")
      .summary("Registers the agent with the router.")
      .in("register")
      .in(jsonBody[ConnectionId])
      .errorOut(httpErrors)
  val registerMediatorServerEndpoint: ZServerEndpoint[DidComm, Any] =
    registerMediator.serverLogicSuccess(_ => ZIO.succeed(()))

  val sendMessage: PublicEndpoint[String, ErrorInfo, Unit, Any] =
    endpoint.post
      .tag("mediator")
      .summary("Mediator service endpoint for sending message")
      .in("message")
      .in(jsonBody[String])
      .errorOut(httpErrors)

  val sendMessageServerEndpoint: ZServerEndpoint[DidComm, Any] = {
    sendMessage.serverLogicSuccess { (base64EncodedString: String) =>
      for {
        _ <- Console.printLine("received message ...")
        msgInMediator <- DidComm.unpackBase64(base64EncodedString)
        _ <- Console.printLine("msgInMediator: ")
        _ <- Console.printLine(msgInMediator.getMessage)
        _ <- Console.printLine("Sending bytes to BOB ...")
        msg = msgInMediator.getMessage.getAttachments().get(0).getData().toJSONObject().get("json").toString()
        _ <- Console.printLine("message to: " + msgInMediator.getMessage.getTo.asScala.toList)
        to <- ZIO.succeed(msgInMediator.getMessage.getTo.asScala.toList.head)
        _ <- Console.printLine("msgToBob: " + msg)
        _ <- ZIO.succeed {
          val msgList: List[String] =
            messages.getOrElse(DidId(to), List.empty[String]) :+ msg
          messages += (DidId(to) -> msgList)
        }
      } yield ()

    }
  }

  val createInvitation: PublicEndpoint[CreateInvitation, ErrorInfo, CreateInvitationResponse, Any] = {
    endpoint.post
      .tag("out-of-band")
      .summary("Create a new DIDCommV2 out of band invitation.")
      .in("invitations")
      .in(jsonBody[CreateInvitation])
      .out(jsonBody[CreateInvitationResponse])
      .errorOut(httpErrors)
      .description("Create Invitation Response")
  }

  val createInvitationServerEndpoint: ZServerEndpoint[DidComm, Any] =
    createInvitation.serverLogicSuccess(invitation =>
      ZIO.succeed(CreateInvitationResponse(invitation.goal, invitation.goal_code))
    )

  val all: List[ZServerEndpoint[DidComm, Any]] =
    List(
      createInvitationServerEndpoint,
      retrieveMessagesServerEndpoint,
      registerMediatorServerEndpoint,
      sendMessageServerEndpoint
    )
}
