package org.hyperledger.identus.didcomm.controller

import io.circe.*
import io.circe.parser.*
import org.hyperledger.identus.agent.server.DidCommHttpServerError.DIDCommMessageParsingError
import org.hyperledger.identus.agent.server.config.AppConfig
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.agent.walletapi.storage.DIDNonSecretStorage
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.connect.core.model.error.ConnectionServiceError
import org.hyperledger.identus.connect.core.service.ConnectionService
import org.hyperledger.identus.didcomm.controller.DIDCommControllerError.*
import org.hyperledger.identus.mercury.*
import org.hyperledger.identus.mercury.DidOps.*
import org.hyperledger.identus.mercury.model.*
import org.hyperledger.identus.mercury.protocol.connection.{ConnectionRequest, ConnectionResponse}
import org.hyperledger.identus.mercury.protocol.issuecredential.*
import org.hyperledger.identus.mercury.protocol.presentproof.*
import org.hyperledger.identus.mercury.protocol.revocationnotificaiton.RevocationNotification
import org.hyperledger.identus.pollux.core.model.error.{CredentialServiceError, PresentationError}
import org.hyperledger.identus.pollux.core.service.{CredentialService, PresentationService}
import org.hyperledger.identus.shared.models.{Failure, StatusCode, WalletAccessContext}
import zio.*

import java.util.UUID
import scala.language.implicitConversions

class DIDCommControllerImpl(
    didOps: DidOps,
    managedDIDService: ManagedDIDService,
    didNonSecretStorage: DIDNonSecretStorage,
    connectionService: ConnectionService,
    presentationService: PresentationService,
    credentialService: CredentialService,
    appConfig: AppConfig
) extends DIDCommController {

  override def handleDIDCommMessage(msg: String)(implicit rc: RequestContext): IO[ErrorResponse, Unit] = {
    for {
      _ <- validateContentType(rc.request.contentType)
      _ <- handleMessage(msg).provideSome(
        ZLayer.succeed(didOps),
        ZLayer.succeed(managedDIDService),
        ZLayer.succeed(didNonSecretStorage),
        ZLayer.succeed(connectionService),
        ZLayer.succeed(credentialService),
        ZLayer.succeed(presentationService),
        ZLayer.succeed(appConfig),
      )
    } yield ()
  }

  private[this] def validateContentType(contentType: Option[String]) = {
    if (contentType.contains(MediaTypes.contentTypeEncrypted)) ZIO.unit
    else ZIO.fail(InvalidContentType(contentType))
  }

  private[this] def handleMessage(msg: String) = {
    ZIO.logAnnotate("request-id", UUID.randomUUID.toString) {
      for {
        _ <- ZIO.logInfo("Received new DIDComm message")
        _ <- ZIO.logTrace(msg)
        msgAndContext <- unpackMessage(msg)
        _ <- processMessage(msgAndContext._1)
          .catchAll {
            case t: Throwable                  => ZIO.die(t) // Convert any 'Throwable' failure to a defect
            case f: Failure                    => ZIO.fail(f)
            case _: DIDCommMessageParsingError => ZIO.fail(UnexpectedError(StatusCode.BadRequest))
            case _: CredentialServiceError     => ZIO.fail(UnexpectedError(StatusCode.UnprocessableContent))
            case _: PresentationError          => ZIO.fail(UnexpectedError(StatusCode.UnprocessableContent))
          }
          .provideSomeLayer(ZLayer.succeed(msgAndContext._2))
      } yield ()
    }
  }

  private def processMessage: PartialFunction[
    Message,
    ZIO[
      PresentationService & WalletAccessContext with CredentialService & WalletAccessContext with ConnectionService &
        WalletAccessContext & AppConfig,
      DIDCommMessageParsingError | ConnectionServiceError | CredentialServiceError | PresentationError | Throwable,
      Any
    ]
  ] = handleConnect orElse
    handleIssueCredential orElse
    handlePresentProof orElse
    revocationNotification orElse
    handleUnknownMessage

  private[this] def unpackMessage(
      jsonString: String
  ): ZIO[DidOps & ManagedDIDService & DIDNonSecretStorage, DIDCommControllerError, (Message, WalletAccessContext)] = {
    for {
      recipientDid <- extractFirstRecipientDid(jsonString).mapError(err => RequestBodyParsingError(err.getMessage))
      _ <- ZIO.logInfo(s"Extracted recipient Did => $recipientDid")
      didId = DidId(recipientDid)
      nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
      maybePeerDIDRecord <- nonSecretStorage.getPeerDIDRecord(didId).orDie
      peerDIDRecord <- ZIO.fromOption(maybePeerDIDRecord).mapError(_ => PeerDIDNotFoundError(didId))
      _ <- ZIO.logInfo(s"PeerDID record successfully loaded in DIDComm receiver endpoint: $peerDIDRecord")
      walletAccessContext = WalletAccessContext(peerDIDRecord.walletId)
      managedDIDService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDIDService
        .getPeerDID(didId)
        .mapError(e => PeerDIDKeyNotFoundError(e.didId, e.keyId))
        .provide(ZLayer.succeed(walletAccessContext))
      agent = AgentPeerService.makeLayer(peerDID)
      msg <- unpack(jsonString).provideSomeLayer(agent)
    } yield (msg.message, walletAccessContext)
  }

  private[this] def extractFirstRecipientDid(jsonMessage: String): IO[ParsingFailure | DecodingFailure, String] = {
    val doc = parse(jsonMessage).getOrElse(Json.Null)
    val cursor = doc.hcursor
    ZIO.fromEither(
      cursor.downField("recipients").downArray.downField("header").downField("kid").as[String].map(_.split("#")(0))
    )
  }

  /*
   * Connect
   */
  private[this] val handleConnect: PartialFunction[Message, ZIO[
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
  private[this] val handleIssueCredential
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
  private[this] val handlePresentProof
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

  private[this] val revocationNotification: PartialFunction[Message, ZIO[Any, Throwable, Unit]] = {
    case msg if msg.piuri == RevocationNotification.`type` =>
      for {
        revocationNotification <- ZIO.attempt(RevocationNotification.readFromMessage(msg))
        _ <- ZIO.logInfo("Got RevocationNotification: " + revocationNotification)
      } yield ()
  }

  /*
   * Unknown Message
   */
  private[this] val handleUnknownMessage: PartialFunction[Message, UIO[String]] = { case _ =>
    ZIO.succeed("Unknown Message Type")
  }
}

object DIDCommControllerImpl {
  val layer: URLayer[
    DidOps & ManagedDIDService & DIDNonSecretStorage & ConnectionService & CredentialService & PresentationService &
      AppConfig,
    DIDCommController
  ] =
    ZLayer.fromFunction(DIDCommControllerImpl(_, _, _, _, _, _, _))
}
