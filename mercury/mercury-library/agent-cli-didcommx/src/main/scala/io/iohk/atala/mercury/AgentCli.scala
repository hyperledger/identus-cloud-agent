package io.iohk.atala.mercury

import scala.jdk.CollectionConverters.*
import zio.*

import zio._
import zio.http._
import zio.http.model._
import zio.http.service._
import java.io.IOException
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.QRcode
import io.iohk.atala.mercury.model.{_, given}
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.mercury.protocol.outofbandlogin._
import io.iohk.atala.mercury.protocol.issuecredential._
import io.iohk.atala.mercury.protocol.presentproof._
import io.iohk.atala.resolvers.PeerDidMediatorSecretResolver
import io.iohk.atala.resolvers.UniversalDidResolver
import io.iohk.atala.mercury.protocol.connection.*
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import io.iohk.atala.resolvers.DIDResolver

/** AgentCli
  * {{{
  *   gentDidcommx/runMain io.iohk.atala.mercury.AgentCli
  * }}}
  */
object AgentCli extends ZIOAppDefault {

  def questionYN(q: String): ZIO[Any, IOException, Boolean] = {
    for {
      _ <- Console.printLine(q + " [y(default)/n] ")
      ret <- Console.readLine.flatMap {
        case "y" | "Y" => ZIO.succeed(true)
        case "n" | "N" => ZIO.succeed(false)
        case ""        => ZIO.succeed(true) // default
        case _         => Console.print("[RETRY] ") *> questionYN(q)
      }
    } yield (ret)
  }

  def options(p: Seq[(String, ZIO[Any, MercuryThrowable, Unit])]): ZIO[Any, MercuryThrowable, Unit] = {
    for {
      _ <- Console.printLine("\n--- Choose an option: ---")
      _ <- ZIO.foreach(p.zipWithIndex)(e => Console.printLine(s"${e._2} - ${e._1._1}"))
      _ <- Console.readLine.flatMap { e => p.map(_._2).toSeq(e.toInt) }
    } yield ()
  }

  val startLogo = Console.printLine("""
      |   ███╗   ███╗███████╗██████╗  ██████╗██╗   ██╗██████╗ ██╗   ██╗       ██████╗██╗     ██╗
      |   ████╗ ████║██╔════╝██╔══██╗██╔════╝██║   ██║██╔══██╗╚██╗ ██╔╝      ██╔════╝██║     ██║
      |   ██╔████╔██║█████╗  ██████╔╝██║     ██║   ██║██████╔╝ ╚████╔╝ █████╗██║     ██║     ██║
      |   ██║╚██╔╝██║██╔══╝  ██╔══██╗██║     ██║   ██║██╔══██╗  ╚██╔╝  ╚════╝██║     ██║     ██║
      |   ██║ ╚═╝ ██║███████╗██║  ██║╚██████╗╚██████╔╝██║  ██║   ██║         ╚██████╗███████╗██║
      |   ╚═╝     ╚═╝╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝          ╚═════╝╚══════╝╚═╝
      |DID Comm V2 Agent - CLI tool for debugging - Build by Atala (IOHK)
      |""".stripMargin)

  // val env = zio.http.Client.default ++ zio.Scope.default

  def agentLayer(peer: PeerDID): ZLayer[Any, Nothing, AgentServiceAny] = ZLayer.succeed(
    io.iohk.atala.mercury.AgentServiceAny(
      new DIDComm(UniversalDidResolver, peer.getSecretResolverInMemory),
      peer.did
    )
  )

  def askForMediation = {
    for {
      _ <- Console.printLine("Enter the Mediator URL (defualt is 'http://localhost:8000')")
      url <- Console.readLine.flatMap {
        case ""  => ZIO.succeed("http://localhost:8000") // defualt
        case str => ZIO.succeed(str)
      }
      _ <- CoordinateMediationPrograms
        .senderMediationRequestProgram(mediatorURL = url)
      // .provideSomeLayer(zio.http.Client.default)
      // .provideSomeLayer(zio.Scope.default)
    } yield ()
  }

  // FIXME create a new MODEL for Login protocol
  def generateLoginInvitation = {
    import io.iohk.atala.mercury.protocol.outofbandlogin._

    // InvitationPrograms.createInvitationV2().map(oob => Response.text(serverUrl + oob))
    for {
      didCommService <- ZIO.service[DidComm]
      invitation = OutOfBandLoginInvitation(from = didCommService.myDid)
      invitationSigned <- didCommService.packSigned(invitation.makeMsg)
      serverUrl = s"https://didcomm-bootstrap.atalaprism.com?_oob=${invitationSigned.base64}" // FIXME locahost
      _ <- Console.printLine(QRcode.getQr(serverUrl).toString)
      _ <- Console.printLine(serverUrl)
      _ <- Console.printLine(invitation.id + " -> " + invitation)
    } yield ()
  }

  def generateConnectionInvitation = {
    import io.iohk.atala.mercury.protocol.invitation._
    for {
      didCommService <- ZIO.service[DidComm]
      invitation = OutOfBandConnection.createInvitation(from = didCommService.myDid)
      serverUrl = s"https://didcomm-bootstrap.atalaprism.com?_oob=${invitation.toBase64}"
      _ <- Console.printLine(serverUrl)
      _ <- Console.printLine(invitation.id + " -> " + invitation)
    } yield ()
  }

  def loginInvitation = {
    import io.iohk.atala.mercury.protocol.outofbandlogin._

    def reaOutOfBandLoginInvitation(msg: org.didcommx.didcomm.message.Message): OutOfBandLoginInvitation = {
      // OutOfBandLoginInvitation(`type` = msg.piuri, id = msg.id, from = msg.from.get)
      OutOfBandLoginInvitation(`type` = msg.getType(), id = msg.getId(), from = DidId(msg.getFrom()))
    }

    for {
      didCommService <- ZIO.service[DidComm]
      _ <- Console.printLine("Read OutOfBand Invitation")
      data <- Console.readLine.flatMap {
        case ""  => ZIO.fail(???) // TODO retry
        case url => ZIO.succeed(Utils.parseLink(url).getOrElse(???)) /// TODO make ERROR
      }
      msg <- didCommService.unpack(data)
      outOfBandLoginInvitation = reaOutOfBandLoginInvitation(msg.getMessage)
      reply = outOfBandLoginInvitation.reply(didCommService.myDid)
      _ <- Console.printLine(s"Replying to ${outOfBandLoginInvitation.id} with $reply")

      res <- MessagingService.send(reply.makeMsg)
      _ <- Console.printLine(res.bodyAsString)
    } yield ()
  }

  def proposeAndSendCredential: ZIO[DidComm & DIDResolver & HttpClient, MercuryError | IOException, Unit] = {
    for {

      _ <- Console.printLine("Propose Credential")
      _ <- Console.printLine("What is the the Playload")
      playloadData <- Console.readLine.flatMap {
        case ""   => ZIO.succeed("playload")
        case data => ZIO.succeed(data)
      }

      attachmentDescriptor =
        AttachmentDescriptor.buildJsonAttachment(payload = playloadData)
      attribute1 = Attribute(name = "name", value = "Joe Blog")
      attribute2 = Attribute(name = "dob", value = "01/10/1947")
      credentialPreview = CredentialPreview(attributes = Seq(attribute1, attribute2))

      didCommService <- ZIO.service[DidComm]
      _ <- Console.printLine(s"Send to (ex: ${didCommService.myDid})")
      sendTo <- Console.readLine.flatMap {
        case ""  => ZIO.succeed(didCommService.myDid)
        case did => ZIO.succeed(DidId(did))
      }

      proposeCredential = ProposeCredential(
        body = ProposeCredential.Body(
          goal_code = Some("goal_code"),
          comment = None,
          credential_preview = credentialPreview, // Option[CredentialPreview], // JSON STRinf
          formats = Seq.empty // : Seq[CredentialFormat]
        ),
        attachments = Seq(attachmentDescriptor),
        from = didCommService.myDid,
        to = sendTo,
      )
      _ <- Console.printLine(proposeCredential)
      msg = proposeCredential.makeMessage
      _ <- Console.printLine("Sending: " + msg)

      _ <- MessagingService.send(msg)
    } yield ()
  }

  def presentProof: ZIO[DidComm & DIDResolver & HttpClient, MercuryError | IOException, Unit] = {
    for {
      _ <- Console.printLine("Present Proof")
      didCommService <- ZIO.service[DidComm]

      _ <- Console.printLine(s"Request proof from did (ex: ${didCommService.myDid})")
      requestTo <- Console.readLine.flatMap {
        case ""  => ZIO.succeed(didCommService.myDid)
        case did => ZIO.succeed(DidId(did))
      }

      // Make a Request
      body = RequestPresentation.Body(goal_code = Some("Propose Presentation"))
      attachmentDescriptor = AttachmentDescriptor(
        "1",
        Some("application/json"),
        LinkData(links = Seq("http://test"), hash = "1234")
      )
      requestPresentation = RequestPresentation(
        body = body,
        attachments = Seq(attachmentDescriptor),
        to = requestTo,
        from = didCommService.myDid,
      )
      msg = requestPresentation.makeMessage
      _ <- Console.printLine("Sending: " + msg)
      _ <- MessagingService.send(msg)
    } yield ()
  }

  def connect: ZIO[DidComm & DIDResolver & HttpClient, MercuryError | IOException, Unit] = {

    import io.iohk.atala.mercury.protocol.invitation.OutOfBand
    import io.circe._, io.circe.parser._
    for {
      didCommService <- ZIO.service[DidComm]
      _ <- Console.printLine("Read OutOfBand Invitation")
      data <- Console.readLine.flatMap {
        case ""  => ZIO.fail(???) // TODO retry
        case url => ZIO.succeed(OutOfBand.parseLink(url).getOrElse(???)) /// TODO make ERROR
      }
      _ <- Console.printLine(s"Decoded Invitation = $data")
      parseResult = parse(data).getOrElse(null)
      connectionInvitation = parseResult.as[Invitation].getOrElse(???)
      _ <- Console.printLine(s"Invitation to ${connectionInvitation.id} with $connectionInvitation")
      connectionRequest = ConnectionRequest(
        from = didCommService.myDid,
        to = connectionInvitation.from,
        thid = Some(connectionInvitation.id), // TODO if this is coorect
        body = ConnectionRequest.Body(goal_code = Some("connect"), goal = Some("Establish Connection"))
      )
      msg = connectionRequest.makeMessage
      _ <- Console.printLine("Sending: " + msg)
      _ <- MessagingService.send(msg)

    } yield ()
  }

  def webServer: HttpApp[DidComm & DIDResolver & HttpClient, Throwable] = {
    val header = "content-type" -> MediaTypes.contentTypeEncrypted
    Http
      .collectZIO[Request] {
        case req @ Method.POST -> !!
            if req.headersAsList
              .exists(h => h._1.toString.equalsIgnoreCase(header._1) && h._2.toString.equalsIgnoreCase(header._2)) =>
          req.body.asString
            .catchNonFatalOrDie(ex => ZIO.fail(ParseResponse(ex)))
            .flatMap { data =>
              webServerProgram(data).catchAll { ex =>
                ZIO.fail(mercuryErrorAsThrowable(ex))
              }
            }
            .map(str => Response.text(str))
        case Method.GET -> !! / "test" => ZIO.succeed(Response.text("Test ok!"))
        case req =>
          ZIO.logWarning(s"Recive a not DID Comm v2 messagem: ${req}") *>
            ZIO.succeed(Response.text(s"The request must be a POST to root with the Header $header"))
      }

  }

  def startEndpoint: ZIO[DidComm & DIDResolver & HttpClient, IOException, Unit] = for {
    _ <- Console.printLine("Setup a endpoint")
    didCommService <- ZIO.service[DidComm]

    defualtPort = UniversalDidResolver
      .resolve(didCommService.myDid.value)
      .get()
      .getDidCommServices()
      .asScala
      .toSeq
      .headOption
      .map(s => s.getServiceEndpoint())
      .flatMap(e => URL.fromString(e).toOption)
      .flatMap(_.port)
      .getOrElse(8081) // defualt

    _ <- Console.printLine(s"Inserte endpoint port ($defualtPort defualt) for (http://localhost:port)")
    port <- Console.readLine.flatMap {
      case ""  => ZIO.succeed(defualtPort)
      case str => ZIO.succeed(str.toIntOption.getOrElse(defualtPort))
    }
    server = {
      val config = ServerConfig(address = new java.net.InetSocketAddress(port))
      ServerConfig.live(config)(using Trace.empty) >>> Server.live
    }
    _ <- Server
      .serve(webServer)
      .provideSomeLayer(server)
      .debug
      .flatMap(e => Console.printLine("Endpoint stop"))
      .catchAll { case ex => Console.printLine(s"Endpoint FAIL ${ex.getMessage()}") }
      .fork
    _ <- Console.printLine(s"Endpoint Started of port '$port'")
  } yield ()

  def run = for {
    _ <- startLogo
    // makeNewDID <- questionYN("Generate new 'peer' DID?")
    _ <- Console.printLine("Generating a new 'peer' DID!")
    // haveServiceEndpoint <- questionYN("Do you have a serviceEndpoint url? e.g http://localhost:8080/myendpoint")
    // ZIO.when(haveServiceEndpoint)( // )
    _ <- Console.printLine("Enter the serviceEndpoint URL (defualt None) or port for http://localhost:port")
    serviceEndpoint <- Console.readLine.flatMap {
      case ""                               => ZIO.succeed(None) // defualt
      case str if str.toIntOption.isDefined => ZIO.succeed(str.toIntOption.map(port => s"http://localhost:$port"))
      case str                              => ZIO.succeed(Some(str))
    }

    agentDID <- for {
      peer <- ZIO.succeed(PeerDID.makePeerDid(serviceEndpoint = serviceEndpoint))
      // jwkForKeyAgreement <- ZIO.succeed(PeerDID.makeNewJwkKeyX25519)
      // jwkForKeyAuthentication <- ZIO.succeed(PeerDID.makeNewJwkKeyEd25519)
      // (jwkForKeyAgreement, jwkForKeyAuthentication)
      _ <- Console.printLine(s"New DID: ${peer.did}") *>
        Console.printLine(s"JWK for KeyAgreement: ${peer.jwkForKeyAgreement.toJSONString}") *>
        Console.printLine(s"JWK for KeyAuthentication: ${peer.jwkForKeyAuthentication.toJSONString}")
    } yield (peer)

    didCommLayer = agentLayer(agentDID)
    layers: ZLayer[Any, Nothing, AgentServiceAny & DIDResolver & HttpClient] =
      didCommLayer ++ DIDResolver.layer ++ ZioHttpClient.layer

    _ <- options(
      Seq(
        "none" -> ZIO.unit,
        "Show DID" -> Console.printLine(agentDID),
        "Get DID Document" -> Console.printLine("DID Document:") *> Console.printLine(agentDID.getDIDDocument),
        "Start WebServer endpoint" -> startEndpoint.provide(layers),
        "Ask for Mediation Coordinate" -> askForMediation.provide(layers),
        "Generate login invitation" -> generateLoginInvitation.provide(didCommLayer),
        "Login with DID" -> loginInvitation.provide(layers),
        "Propose Credential" -> proposeAndSendCredential.provide(layers),
        "Present Proof" -> presentProof.provide(layers),
        "Generate Connection invitation" -> generateConnectionInvitation.provide(didCommLayer),
        "Connect" -> connect.provide(layers),
      )
    ).repeatWhile((_) => true)

  } yield ()

  def webServerProgram(
      jsonString: String
  ): ZIO[DidComm & DIDResolver & HttpClient, MercuryThrowable, String] = { // TODO Throwable
    import io.iohk.atala.mercury.DidComm.*
    ZIO.logAnnotate("request-id", java.util.UUID.randomUUID.toString()) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(jsonString)
        msg <- unpack(jsonString).map(_.getMessage)
        ret <- {
          msg.getType match {
            case s if s == OutOfBandloginReply.piuri =>
              for {
                _ <- ZIO.logInfo("OutOfBandloginReply: " + msg)
              } yield ("OutOfBandloginReply")

            // ########################
            // ### issue-credential ###
            // ########################
            case s if s == ProposeCredential.`type` => // Issuer
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Issuer in issue-credential:")
                _ <- ZIO.logInfo("Got ProposeCredential: " + msg)
                offer = OfferCredential.makeOfferToProposeCredential(msg) // OfferCredential

                didCommService <- ZIO.service[DidComm]
                msgToSend = offer.makeMessage
                _ <- MessagingService.send(msgToSend)
              } yield ("OfferCredential Sent")

            case s if s == OfferCredential.`type` => // Holder
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Holder in issue-credential:")
                _ <- ZIO.logInfo("Got OfferCredential: " + msg)
                // store on BD TODO //pc = OfferCredential.readFromMessage(msg)
                requestCredential = RequestCredential.makeRequestCredentialFromOffer(msg) // RequestCredential

                didCommService <- ZIO.service[DidComm]
                msgToSend = requestCredential.makeMessage
                _ <- MessagingService.send(msgToSend)
              } yield ("RequestCredential Sent")

            case s if s == RequestCredential.`type` => // Issuer
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Issuer in issue-credential:")
                _ <- ZIO.logInfo("Got RequestCredential: " + msg)
                issueCredential = IssueCredential.makeIssueCredentialFromRequestCredential(msg) // IssueCredential

                didCommService <- ZIO.service[DidComm]
                msgToSend = issueCredential.makeMessage
                _ <- MessagingService.send(msgToSend)
              } yield ("IssueCredential Sent")

            case s if s == IssueCredential.`type` => // Holder
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Holder in issue-credential:")
                _ <- ZIO.logInfo("Got IssueCredential: " + msg)
              } yield ("IssueCredential Received")
            // ######################################################################
            case s if s == RequestPresentation.`type` => // Prover
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Prover in Present-Proof:")
                requestPresentation = RequestPresentation.readFromMessage(msg)
                _ <- ZIO.logInfo("Got RequestPresentation: " + requestPresentation)
                presentation = Presentation.makePresentationFromRequest(msg)
                didCommService <- ZIO.service[DidComm]
                msgToSend = presentation.makeMessage
                _ <- MessagingService.send(msgToSend)
              } yield ("Presentation Sent")
            case s if s == Presentation.`type` => // Verifier
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Verifier in Present-Proof:")
                presentation = Presentation.readFromMessage(msg)
                _ <- ZIO.logInfo("Got Presentation: " + presentation)
              } yield ("Presentation Recived")
            // ########################Comnnect##############################################
            case s if s == ConnectionRequest.`type` => // Inviter
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As Inviter in Connect:")
                connectionRequest = ConnectionRequest.readFromMessage(msg)
                _ <- ZIO.logInfo("Got ConnectionRequest: " + connectionRequest)
                _ <- ZIO.logInfo("Creating New PeerDID...")
//                peer <- ZIO.succeed(PeerDID.makePeerDid(serviceEndpoint = serviceEndpoint)) TODO
//                _ <- ZIO.logInfo(s"My new DID => $peer")
                connectionResponse = ConnectionResponse.makeResponseFromRequest(msg)
                msgToSend = connectionResponse.makeMessage
                _ <- MessagingService.send(msgToSend)
              } yield ("Connection Request Sent")
            case s if s == ConnectionResponse.`type` => // Invitee
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As Invitee in Connect:")
                connectionResponse = ConnectionResponse.readFromMessage(msg)
                _ <- ZIO.logInfo("Got Connection Response: " + connectionResponse)
              } yield ("Connection established")

            case "https://didcomm.org/routing/2.0/forward"                      => ??? // SEE mediator
            case "https://atalaprism.io/mercury/mailbox/1.0/ReadMessages"       => ??? // SEE mediator
            case "https://didcomm.org/coordinate-mediation/2.0/mediate-request" => ??? // SEE mediator
            case _                                                              => ZIO.succeed("Unknown Message Type")
          }
        }
      } yield (ret)
    }
  }

}
