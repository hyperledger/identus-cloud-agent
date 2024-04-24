package org.hyperledger.identus.agent.server

import io.circe.*
import io.circe.parser.*
import org.hyperledger.identus.agent.server.DidCommHttpServerError.{
  DIDCommMessageParsingError,
  InvalidContentTypeError,
  RequestBodyParsingError
}
import org.hyperledger.identus.agent.server.config.AppConfig
import org.hyperledger.identus.agent.walletapi.model.error.DIDSecretStorageError
import org.hyperledger.identus.agent.walletapi.model.error.DIDSecretStorageError.{KeyNotFoundError, WalletNotFoundError}
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.agent.walletapi.storage.DIDNonSecretStorage
import org.hyperledger.identus.connect.core.model.error.ConnectionServiceError
import org.hyperledger.identus.connect.core.service.ConnectionService
import org.hyperledger.identus.mercury.*
import org.hyperledger.identus.mercury.DidOps.*
import org.hyperledger.identus.mercury.model.*
import org.hyperledger.identus.mercury.error.*
import org.hyperledger.identus.mercury.protocol.connection.{ConnectionRequest, ConnectionResponse}
import org.hyperledger.identus.mercury.protocol.issuecredential.*
import org.hyperledger.identus.mercury.protocol.presentproof.*
import org.hyperledger.identus.mercury.protocol.revocationnotificaiton.RevocationNotification
import org.hyperledger.identus.pollux.core.model.error.{CredentialServiceError, PresentationError}
import org.hyperledger.identus.pollux.core.service.{CredentialService, PresentationService}
import org.hyperledger.identus.resolvers.DIDResolver
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*
import zio.http.*
import java.util.UUID

object DidCommHttpServer {

  def run = {
    def server(didCommServicePort: Int) = {
      val config = Server.Config.default.copy(address = new java.net.InetSocketAddress(didCommServicePort))
      ZLayer.succeed(config) >>> Server.live
    }
    for {
      appConfig <- ZIO.service[AppConfig]
      didCommServicePort = appConfig.agent.didCommEndpoint.http.port
      _ <- ZIO.logInfo(s"Server Started on port $didCommServicePort")
      _ <- Server
        .serve(didCommServiceEndpoint)
        .provideSomeLayer(server(didCommServicePort))
        .debug *> ZIO
        .logWarning(s"Server STOP (on port $didCommServicePort)")
        .tapDefect(error => ZIO.logErrorCause("Defect processing incoming DIDComm message", Cause.fail(error)))
    } yield ()
  }

  private def validateContentType(req: Request) = {
    import zio.http.Header.ContentType
    for {
      contentType <- ZIO
        .fromOption(req.rawHeader(ContentType.name))
        .mapError(_ => InvalidContentTypeError(s"The '${ContentType.name}' header is required"))
      _ <-
        if (contentType.equalsIgnoreCase(MediaTypes.contentTypeEncrypted)) ZIO.unit
        else ZIO.fail(InvalidContentTypeError(s"Unsupported '${ContentType.name}' header value: $contentType"))
    } yield contentType
  }

  private def didCommServiceEndpoint: HttpApp[
    DidOps & CredentialService & PresentationService & ConnectionService & ManagedDIDService & HttpClient &
      DIDResolver & DIDNonSecretStorage & AppConfig
  ] =
    val rootRoute = Method.POST / "" -> handler { (req: Request) =>
      val result = for {
        _ <- validateContentType(req)
        body <- req.body.asString.mapError(e => RequestBodyParsingError(e.getMessage))
        _ <- webServerProgram(body)
      } yield Response.ok
      result
        .tapError(error => ZIO.logErrorCause("Error processing incoming DIDComm message", Cause.fail(error)))
        .catchAll {
          case _: RequestBodyParsingError    => ZIO.succeed(Response.status(Status.BadRequest))
          case _: InvalidContentTypeError    => ZIO.succeed(Response.status(Status.BadRequest))
          case _: DIDCommMessageParsingError => ZIO.succeed(Response.status(Status.BadRequest))
          case _: ParseResponse              => ZIO.succeed(Response.status(Status.BadRequest))
          case _: DIDSecretStorageError      => ZIO.succeed(Response.status(Status.UnprocessableEntity))
          case _: ConnectionServiceError     => ZIO.succeed(Response.status(Status.UnprocessableEntity))
          case _: CredentialServiceError     => ZIO.succeed(Response.status(Status.UnprocessableEntity))
          case _: PresentationError          => ZIO.succeed(Response.status(Status.UnprocessableEntity))
        }
    }
    Routes(rootRoute).toHttpApp

  private[this] def extractFirstRecipientDid(jsonMessage: String): IO[ParsingFailure | DecodingFailure, String] = {
    val doc = parse(jsonMessage).getOrElse(Json.Null)
    val cursor = doc.hcursor
    ZIO.fromEither(
      cursor.downField("recipients").downArray.downField("header").downField("kid").as[String].map(_.split("#")(0))
    )
  }

  private[this] def unpackMessage(
      jsonString: String
  ): ZIO[
    DidOps & ManagedDIDService & DIDNonSecretStorage,
    ParseResponse | DIDSecretStorageError,
    (Message, WalletAccessContext)
  ] = {
    // Needed for implicit conversion from didcommx UnpackResuilt to mercury UnpackMessage
    for {
      recipientDid <- extractFirstRecipientDid(jsonString).mapError(err => ParseResponse(err))
      _ <- ZIO.logInfo(s"Extracted recipient Did => $recipientDid")
      didId = DidId(recipientDid)
      nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
      maybePeerDIDRecord <- nonSecretStorage.getPeerDIDRecord(didId).orDie
      peerDIDRecord <- ZIO.fromOption(maybePeerDIDRecord).mapError(_ => WalletNotFoundError(didId))
      _ <- ZIO.logInfo(s"PeerDID record successfully loaded in DIDComm receiver endpoint: $peerDIDRecord")
      walletAccessContext = WalletAccessContext(peerDIDRecord.walletId)
      managedDIDService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDIDService.getPeerDID(didId).provide(ZLayer.succeed(walletAccessContext))
      agent = AgentPeerService.makeLayer(peerDID)
      msg <- unpack(jsonString).provideSomeLayer(agent)
    } yield (msg.message, walletAccessContext)
  }

  private def webServerProgram(jsonString: String) = {
    ZIO.logAnnotate("request-id", UUID.randomUUID.toString) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(jsonString)
        msgAndContext <- unpackMessage(jsonString)
        _ <- (handleConnect orElse
          handleIssueCredential orElse
          handlePresentProof orElse
          revocationNotification orElse
          handleUnknownMessage)(msgAndContext._1).provideSomeLayer(ZLayer.succeed(msgAndContext._2))
      } yield ()
    }
  }

  /*
   * Connect
   */
  private val handleConnect: PartialFunction[Message, ZIO[
    ConnectionService & WalletAccessContext & AppConfig,
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
  private val handleIssueCredential
      : PartialFunction[Message, ZIO[CredentialService & WalletAccessContext, CredentialServiceError, Unit]] = {
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
  private val handlePresentProof
      : PartialFunction[Message, ZIO[PresentationService & WalletAccessContext, PresentationError, Unit]] = {
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

  private val revocationNotification: PartialFunction[Message, ZIO[Any, Throwable, Unit]] = {
    case msg if msg.piuri == RevocationNotification.`type` =>
      for {
        revocationNotification <- ZIO.attempt(RevocationNotification.readFromMessage(msg))
        _ <- ZIO.logInfo("Got RevocationNotification: " + revocationNotification)
      } yield ()
  }

  /*
   * Unknown Message
   */
  private val handleUnknownMessage: PartialFunction[Message, UIO[String]] = { case _ =>
    ZIO.succeed("Unknown Message Type")
  }

}
