package io.iohk.atala.agent.server

import io.circe.*
import io.circe.parser.*
import io.iohk.atala.agent.server.DidCommHttpServerError.{DIDCommMessageParsingError, RequestBodyParsingError}
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.mercury.*
import io.iohk.atala.mercury.DidOps.*
import io.iohk.atala.mercury.model.*
import io.iohk.atala.mercury.model.error.*
import io.iohk.atala.mercury.protocol.connection.{ConnectionRequest, ConnectionResponse}
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.mercury.protocol.presentproof.*
import io.iohk.atala.mercury.protocol.trustping.TrustPing
import io.iohk.atala.pollux.core.model.error.{CredentialServiceError, PresentationError}
import io.iohk.atala.pollux.core.service.{CredentialService, PresentationService}
import io.iohk.atala.resolvers.DIDResolver
import zio.*
import zio.http.*
import zio.http.model.*

import java.util.UUID

object DidCommHttpServer {

  def run(didCommServicePort: Int) = {
    val server = {
      val config = ServerConfig(address = new java.net.InetSocketAddress(didCommServicePort))
      ServerConfig.live(config)(using Trace.empty) >>> Server.live
    }
    for {
      _ <- ZIO.logInfo(s"Server Started on port $didCommServicePort")
      _ <- Server
        .serve(didCommServiceEndpoint)
        .provideSomeLayer(server)
        .debug *> ZIO.logWarning(s"Server STOP (on port $didCommServicePort)")
    } yield ()
  }

  private def didCommServiceEndpoint: HttpApp[
    DidOps & DidAgent & CredentialService & PresentationService & ConnectionService & ManagedDIDService & HttpClient &
      DidAgent & DIDResolver & AppConfig,
    Nothing
  ] = Http.collectZIO[Request] {
    case Method.GET -> !! / "did" =>
      for {
        didCommService <- ZIO.service[DidAgent]
        str = didCommService.id.value
      } yield Response.text(str)

    case req @ Method.POST -> !!
        if req.headersAsList
          .exists(h =>
            h.key.toString.equalsIgnoreCase("content-type") &&
              h.value.toString.equalsIgnoreCase(MediaTypes.contentTypeEncrypted)
          ) =>
      val result = for {
        data <- req.body.asString.mapError(e => RequestBodyParsingError(e.getMessage))
        _ <- webServerProgram(data)
      } yield Response.ok

      result
        .tapError(error => ZIO.logErrorCause("Error processing incoming DIDComm message", Cause.fail(error)))
        .catchAll {
          case _: RequestBodyParsingError    => ZIO.succeed(Response.status(Status.BadRequest))
          case _: DIDCommMessageParsingError => ZIO.succeed(Response.status(Status.BadRequest))
          case _: ParseResponse              => ZIO.succeed(Response.status(Status.BadRequest))
          case _: DIDSecretStorageError      => ZIO.succeed(Response.status(Status.UnprocessableEntity))
          case _: SendMessageError           => ZIO.succeed(Response.status(Status.UnprocessableEntity))
          case _: ConnectionServiceError     => ZIO.succeed(Response.status(Status.UnprocessableEntity))
          case _: CredentialServiceError     => ZIO.succeed(Response.status(Status.UnprocessableEntity))
          case _: PresentationError          => ZIO.succeed(Response.status(Status.UnprocessableEntity))
        }

  }

  private[this] def extractFirstRecipientDid(jsonMessage: String): IO[ParsingFailure | DecodingFailure, String] = {
    val doc = parse(jsonMessage).getOrElse(Json.Null)
    val cursor = doc.hcursor
    ZIO.fromEither(
      cursor.downField("recipients").downArray.downField("header").downField("kid").as[String].map(_.split("#")(0))
    )
  }

  private[this] def unpackMessage(
      jsonString: String
  ): ZIO[DidOps & ManagedDIDService, ParseResponse | DIDSecretStorageError, Message] = {
    // Needed for implicit conversion from didcommx UnpackResuilt to mercury UnpackMessage
    for {
      recipientDid <- extractFirstRecipientDid(jsonString).mapError(err => ParseResponse(err))
      _ <- ZIO.logInfo(s"Extracted recipient Did => $recipientDid")
      managedDIDService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDIDService.getPeerDID(DidId(recipientDid))
      agent = AgentPeerService.makeLayer(peerDID)
      msg <- unpack(jsonString).provideSomeLayer(agent)
    } yield msg.message
  }

  private def webServerProgram(jsonString: String) = {
    ZIO.logAnnotate("request-id", UUID.randomUUID.toString) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(jsonString)
        msg <- unpackMessage(jsonString)
        _ <- (handleTrustPing orElse
          handleConnect orElse
          handleIssueCredential orElse
          handlePresentProof orElse
          handleUnknownMessage)(msg)
      } yield ()
    }
  }

  /*
   * Trust Ping
   */
  private val handleTrustPing
      : PartialFunction[Message, ZIO[DidOps & DidAgent & DIDResolver & HttpClient, SendMessageError, Unit]] = {
    case msg if msg.piuri == TrustPing.`type` =>
      for {
        trustPingMsg <- ZIO.succeed(TrustPing.fromMessage(msg))
        _ <- ZIO.logInfo(s"Got TrustPing from ${msg.from}: $trustPingMsg")
        trustPingResponseMsg = trustPingMsg match {
          case Left(value) => None
          case Right(trustPing) =>
            trustPing.body.response_requested match
              case None        => Some(trustPing.makeReply.makeMessage)
              case Some(true)  => Some(trustPing.makeReply.makeMessage)
              case Some(false) => None
        }
        _ <- trustPingResponseMsg match
          case None => ZIO.logWarning(s"Did not reply to the ${TrustPing.`type`}")
          case Some(message) =>
            MessagingService
              .send(message)
              .flatMap(response =>
                response.status match
                  case c if c >= 200 & c < 300 => ZIO.unit
                  case _                       => ZIO.logWarning(response.toString)
              )
      } yield ()
  }

  /*
   * Connect
   */
  private val handleConnect: PartialFunction[Message, ZIO[
    ConnectionService & AppConfig,
    DIDCommMessageParsingError | ConnectionServiceError,
    Unit
  ]] = {
    case msg if msg.piuri == ConnectionRequest.`type` =>
      for {
        connectionRequest <- ZIO
          .fromEither(ConnectionRequest.fromMessage(msg))
          .mapError(DIDCommMessageParsingError.apply)
        _ <- ZIO.logInfo("As an Inviter in connect got ConnectionRequest: " + connectionRequest)
        connectionService <- ZIO.service[ConnectionService]
        config <- ZIO.service[AppConfig]
        record <- connectionService.receiveConnectionRequest(
          connectionRequest,
          Some(config.connect.connectInvitationExpiry)
        )
        _ <- connectionService.acceptConnectionRequest(record.id)
      } yield ()
    case msg if msg.piuri == ConnectionResponse.`type` =>
      for {
        connectionResponse <- ZIO
          .fromEither(ConnectionResponse.fromMessage(msg))
          .mapError(DIDCommMessageParsingError.apply)
        _ <- ZIO.logInfo("As an Invitee in connect got ConnectionResponse: " + connectionResponse)
        connectionService <- ZIO.service[ConnectionService]
        _ <- connectionService.receiveConnectionResponse(connectionResponse)
      } yield ()
  }

  /*
   * Issue Credential
   */
  private val handleIssueCredential: PartialFunction[Message, ZIO[CredentialService, CredentialServiceError, Unit]] = {
    case msg if msg.piuri == OfferCredential.`type` =>
      for {
        offerFromIssuer <- ZIO.succeed(OfferCredential.readFromMessage(msg))
        _ <- ZIO.logInfo("As an Holder in issue-credential got OfferCredential: " + offerFromIssuer)
        credentialService <- ZIO.service[CredentialService]
        _ <- credentialService.receiveCredentialOffer(offerFromIssuer)
      } yield ()
    case msg if msg.piuri == RequestCredential.`type` =>
      for {
        requestCredential <- ZIO.succeed(RequestCredential.readFromMessage(msg))
        _ <- ZIO.logInfo("As an Issuer in issue-credential got RequestCredential: " + requestCredential)
        credentialService <- ZIO.service[CredentialService]
        _ <- credentialService.receiveCredentialRequest(requestCredential)
      } yield ()
    case msg if msg.piuri == IssueCredential.`type` =>
      for {
        issueCredential <- ZIO.succeed(IssueCredential.readFromMessage(msg))
        _ <- ZIO.logInfo("As an Holder in issue-credential got IssueCredential: " + issueCredential)
        credentialService <- ZIO.service[CredentialService]
        _ <- credentialService.receiveCredentialIssue(issueCredential)
      } yield ()
  }

  /*
   * Present Proof
   */
  private val handlePresentProof: PartialFunction[Message, ZIO[PresentationService, PresentationError, Unit]] = {
    case msg if msg.piuri == ProposePresentation.`type` =>
      for {
        proposePresentation <- ZIO.succeed(ProposePresentation.readFromMessage(msg))
        _ <- ZIO.logInfo("As a Verifier in  present-proof got ProposePresentation: " + proposePresentation)
        service <- ZIO.service[PresentationService]
        _ <- service.receiveProposePresentation(proposePresentation)
      } yield ()
    case msg if msg.piuri == RequestPresentation.`type` =>
      for {
        requestPresentation <- ZIO.succeed(RequestPresentation.readFromMessage(msg))
        _ <- ZIO.logInfo("As a Prover in present-proof got RequestPresentation: " + requestPresentation)
        service <- ZIO.service[PresentationService]
        _ <- service.receiveRequestPresentation(None, requestPresentation)
      } yield ()
    case msg if msg.piuri == Presentation.`type` =>
      for {
        presentation <- ZIO.succeed(Presentation.readFromMessage(msg))
        _ <- ZIO.logInfo("As a Verifier in present-proof got Presentation: " + presentation)
        service <- ZIO.service[PresentationService]
        _ <- service.receivePresentation(presentation)
      } yield ()
  }

  /*
   * Unknown Message
   */
  private val handleUnknownMessage: PartialFunction[Message, UIO[String]] = { case _ =>
    ZIO.succeed("Unknown Message Type")
  }

}
