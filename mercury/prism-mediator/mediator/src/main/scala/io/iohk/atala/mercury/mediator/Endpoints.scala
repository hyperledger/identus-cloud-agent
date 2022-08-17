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

<<<<<<< Updated upstream
=======
  type DID
  // val messages = mutable.Map[ConnectionId, List[String]]()
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
  val sendMessageServerEndpoint: ZServerEndpoint[DidComm, Any] = {
    val sendMessage: PublicEndpoint[String, ErrorInfo, Unit, Any] =
      endpoint.post
        .tag("mediator")
        .summary("Mediator service endpoint for sending message")
        .in("message")
        .in(stringBody)
        .errorOut(httpErrors)
    sendMessage.serverLogicSuccess { (base64EncodedString: String) => MediatorProgram.program(base64EncodedString) }
=======
  val sendMessage: PublicEndpoint[String, ErrorInfo, String, Any] =
    endpoint.post
      .tag("mediator")
      .summary("Mediator service endpoint for sending message")
      .in("message")
      .in(stringBody)
      .out(jsonBody[String])
      .errorOut(httpErrors)

  val sendMessageServerEndpoint: ZServerEndpoint[DidComm, Any] = {
    sendMessage.serverLogicSuccess { (base64EncodedString: String) =>
      for {
        _ <- Console.printLine(s"received message ...\n $base64EncodedString \n")
        msgInMediator <- DidComm.unpackBase64(base64EncodedString)
        _ <- Console.printLine("msgInMediator: ")
        _ <- Console.printLine(msgInMediator.getMessage)
        result <- ZIO.succeed(messageProcessing(msgInMediator.getMessage))

      } yield (result)

    }
  }

  def messageProcessing(message: org.didcommx.didcomm.message.Message): String = {
    val typeOfMessage = message.getType
    val recipient = message.getTo.asScala.toList.head
    val did = DidId(recipient)
    typeOfMessage match {
      case "https://didcomm.org/routing/2.0/forward" => {
        val msg = message.getAttachments().get(0).getData().toJSONObject().get("json").toString()
        val msgList: List[String] =
          messages.getOrElse(did, List.empty[String]) :+ msg
        messages += (did -> msgList)
        "Message Forwarded"
      }
      case "https://atalaprism.io/mercury/mailbox/1.0/ReadMessages" =>
        messages.getOrElse(did, List.empty[String]).toString()
      case _ => "Unknown Message Type"
    }
>>>>>>> Stashed changes
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

  val createInvitationServerEndpoint: ZServerEndpoint[Any, Any] =
    createInvitation.serverLogicSuccess(invitation =>
      ZIO.succeed(CreateInvitationResponse(invitation.goal, invitation.goal_code))
    )

  val all: List[ZServerEndpoint[DidComm, Any]] =
    List(
      createInvitationServerEndpoint.widen[DidComm],
      retrieveMessagesServerEndpoint.widen[DidComm],
      registerMediatorServerEndpoint.widen[DidComm],
      sendMessageServerEndpoint
    )
}
