package io.iohk.atala.agent.server

import io.circe.DecodingFailure
import io.circe.ParsingFailure
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
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.core.service.PresentationService
import io.iohk.atala.resolvers.DIDResolver
import java.io.IOException
import zio.*
import zio.http.*
import zio.http.model.*

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
      DidAgent & DIDResolver,
    Throwable
  ] = Http.collectZIO[Request] {
    case Method.GET -> !! / "did" =>
      for {
        didCommService <- ZIO.service[DidAgent]
        str = didCommService.id.value
      } yield (Response.text(str))

    case req @ Method.POST -> !!
        if req.headersAsList
          .exists(h =>
            h.key.toString.equalsIgnoreCase("content-type") &&
              h.value.toString.equalsIgnoreCase(MediaTypes.contentTypeEncrypted)
          ) =>
      val result = for {
        data <- req.body.asString
        _ <- webServerProgram(data)
      } yield Response.ok

      result
        .tapError { error =>
          ZIO.logErrorCause("Fail to POST form webServerProgram", Cause.fail(error))
        }
        .mapError {
          case ex: DIDSecretStorageError => ex
          case ex: MercuryThrowable      => mercuryErrorAsThrowable(ex)
        }
  }

  private[this] def extractFirstRecipientDid(jsonMessage: String): IO[ParsingFailure | DecodingFailure, String] = {
    import io.circe.*
    import io.circe.parser.*
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
    import io.iohk.atala.mercury.model.given
    for {
      recipientDid <- extractFirstRecipientDid(jsonString).mapError(err => ParseResponse(err))
      _ <- ZIO.logInfo(s"Extracted recipient Did => $recipientDid")
      managedDIDService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDIDService.getPeerDID(DidId(recipientDid))
      agent = AgentPeerService.makeLayer(peerDID)
      msg <- unpack(jsonString).provideSomeLayer(agent)
    } yield msg.message
  }

  private def webServerProgram(jsonString: String): ZIO[
    DidOps & CredentialService & PresentationService & ConnectionService & ManagedDIDService & HttpClient & DidAgent &
      DIDResolver,
    MercuryThrowable | DIDSecretStorageError,
    Unit
  ] = {

    ZIO.logAnnotate("request-id", java.util.UUID.randomUUID.toString()) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(jsonString)
        msg <- unpackMessage(jsonString)
        credentialService <- ZIO.service[CredentialService]
        connectionService <- ZIO.service[ConnectionService]
        _ <- {
          msg.piuri match {
            // ##################
            // ### Trust-Ping ###
            // ##################
            case s if s == TrustPing.`type` =>
              for {
                _ <- ZIO.logInfo("*" * 100)
                trustPingMsg = TrustPing.fromMessage(msg)
                _ <- ZIO.logInfo(s"TrustPing from ${msg.from}: " + msg + " -> " + trustPingMsg)
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
                          case c                       => ZIO.logWarning(response.toString())
                      )
              } yield ()

            // ########################
            // ### issue-credential ###
            // ########################
            case s if s == ProposeCredential.`type` => // Issuer
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Issuer in issue-credential:")
                _ <- ZIO.logInfo("Got ProposeCredential: " + msg)
                credentialService <- ZIO.service[CredentialService]

                // TODO
              } yield ()

            case s if s == OfferCredential.`type` => // Holder
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Holder in issue-credential:")
                _ <- ZIO.logInfo("Got OfferCredential: " + msg)
                offerFromIssuer = OfferCredential.readFromMessage(msg)
                _ <- credentialService
                  .receiveCredentialOffer(offerFromIssuer)
                  .catchSome { case CredentialServiceError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }

              } yield ()

            case s if s == RequestCredential.`type` => // Issuer
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Issuer in issue-credential:")
                requestCredential = RequestCredential.readFromMessage(msg)
                _ <- ZIO.logInfo("Got RequestCredential: " + requestCredential)
                credentialService <- ZIO.service[CredentialService]
                todoTestOption <- credentialService
                  .receiveCredentialRequest(requestCredential)
                  .catchSome { case CredentialServiceError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }

                // TODO todoTestOption if none
              } yield ()

            case s if s == IssueCredential.`type` => // Holder
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Holder in issue-credential:")
                issueCredential = IssueCredential.readFromMessage(msg)
                _ <- ZIO.logInfo("Got IssueCredential: " + issueCredential)
                credentialService <- ZIO.service[CredentialService]
                _ <- credentialService
                  .receiveCredentialIssue(issueCredential)
                  .catchSome { case CredentialServiceError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()

            // #####################
            // ### present-proof ###
            // #####################

            case s if s == ProposePresentation.`type` =>
              for {
                _ <- ZIO.unit
                request = ProposePresentation.readFromMessage(msg)
                _ <- ZIO.logInfo("As a Verifier in  present-proof got ProposePresentation: " + request)
                service <- ZIO.service[PresentationService]
                _ <- service
                  .receiveProposePresentation(request)
                  .catchSome { case PresentationError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *> ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()

            case s if s == RequestPresentation.`type` =>
              for {
                _ <- ZIO.unit
                request = RequestPresentation.readFromMessage(msg)
                _ <- ZIO.logInfo("As a Prover in present-proof got RequestPresentation: " + request)
                service <- ZIO.service[PresentationService]
                _ <- service
                  .receiveRequestPresentation(None, request)
                  .catchSome { case PresentationError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *> ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()
            case s if s == Presentation.`type` =>
              for {
                _ <- ZIO.unit
                request = Presentation.readFromMessage(msg)
                _ <- ZIO.logInfo("As a Verifier in present-proof got Presentation: " + request)
                service <- ZIO.service[PresentationService]
                _ <- service
                  .receivePresentation(request)
                  .catchSome { case PresentationError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *> ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()

            case s if s == ConnectionRequest.`type` =>
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Inviter in connect:")
                connectionRequest <- ConnectionRequest.fromMessage(msg) match {
                  case Left(error)  => ZIO.fail(new RuntimeException(error))
                  case Right(value) => ZIO.succeed(value)
                }
                _ <- ZIO.logInfo("Got ConnectionRequest: " + connectionRequest)
                // Receive and store ConnectionRequest
                maybeRecord <- connectionService
                  .receiveConnectionRequest(connectionRequest)
                  .catchSome { case ConnectionServiceError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
                // Accept the ConnectionRequest
                _ <- connectionService
                  .acceptConnectionRequest(maybeRecord.id)
                  .catchSome { case ConnectionServiceError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()

            // As an Invitee, I received a ConnectionResponse from an Inviter who replied to my ConnectionRequest.
            case s if s == ConnectionResponse.`type` =>
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Invitee in connect:")
                connectionResponse <- ConnectionResponse.fromMessage(msg) match {
                  case Left(error)  => ZIO.fail(new RuntimeException(error))
                  case Right(value) => ZIO.succeed(value)
                }
                _ <- ZIO.logInfo("Got ConnectionResponse: " + connectionResponse)
                _ <- connectionService
                  .receiveConnectionResponse(connectionResponse)
                  .catchSome { case ConnectionServiceError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()

            case _ => ZIO.succeed("Unknown Message Type")
          }
        }
      } yield ()
    }
  }

}
