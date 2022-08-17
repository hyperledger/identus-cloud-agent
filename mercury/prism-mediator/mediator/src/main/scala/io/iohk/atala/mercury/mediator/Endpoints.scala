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
import io.iohk.atala.mercury.protocol.mailbox.Mailbox.`type`
import io.iohk.atala.mercury.protocol.routing.ForwardMessage

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
    .in(header[String]("did"))
    .description("did in the header")
    .out(jsonBody[List[String]])

  val retrieveMessagesServerEndpoint: ZServerEndpoint[Any, Any] =
    retrieveMessages.serverLogicSuccess(id => ZIO.succeed(messages.getOrElse(DidId(id), List.empty[String])))

  val registerMediator: PublicEndpoint[ConnectionId, ErrorInfo, Unit, Any] =
    endpoint.post
      .tag("mediator")
      .summary("Registers the agent with the router.")
      .in("register")
      .in(jsonBody[ConnectionId])
      .errorOut(httpErrors)
  val registerMediatorServerEndpoint: ZServerEndpoint[Any, Any] =
    registerMediator.serverLogicSuccess(_ => ZIO.succeed(()))

  val sendMessageServerEndpoint: ZServerEndpoint[DidComm & ZDB, Any] = {
    val sendMessage: PublicEndpoint[String, ErrorInfo, String, Any] =
      endpoint.post
        .tag("mediator")
        .summary("Mediator service endpoint for sending message")
        .in("message")
        .in(stringBody)
        .out(jsonBody[String])
        .errorOut(httpErrors)
    sendMessage.serverLogicSuccess { (base64EncodedString: String) =>
      MediatorProgram.program(base64EncodedString)

    }
  }

  // val sendMessageServerEndpoint: ZServerEndpoint[DidComm, Any] = {
  //   sendMessage.serverLogicSuccess { (base64EncodedString: String) =>
  //     for {
  //       _ <- Console.printLine(s"received message ...\n $base64EncodedString \n")
  //       msgInMediator <- DidComm.unpackBase64(base64EncodedString)
  //       _ <- Console.printLine("msgInMediator: ")
  //       _ <- Console.printLine(msgInMediator.getMessage)
  //       result <- ZIO.succeed(messageProcessing(msgInMediator.getMessage))

  //     } yield (result)

  //   }
  // }

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

  val createInvitationServerEndpoint: ZServerEndpoint[Any, Any] =
    createInvitation.serverLogicSuccess(invitation =>
      ZIO.succeed(CreateInvitationResponse(invitation.goal, invitation.goal_code))
    )

  val all: List[ZServerEndpoint[DidComm & ZDB, Any]] =
    List(
      createInvitationServerEndpoint.widen[DidComm & ZDB],
      retrieveMessagesServerEndpoint.widen[DidComm & ZDB],
      registerMediatorServerEndpoint.widen[DidComm & ZDB],
      sendMessageServerEndpoint
    )
}
