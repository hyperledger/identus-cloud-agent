package io.iohk.atala.mercury

import scala.jdk.CollectionConverters.*
import zio.*
import zio.http.{Header as _, *}
import java.io.IOException
import io.iohk.atala.QRcode
import io.iohk.atala.mercury._
import io.iohk.atala.mercury.model.{given, _}
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.mercury.protocol.outofbandlogin._
import io.iohk.atala.mercury.protocol.issuecredential._
import io.iohk.atala.mercury.protocol.presentproof._
import io.iohk.atala.mercury.protocol.connection.*
import io.iohk.atala.mercury.protocol.reportproblem.v2._
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import io.iohk.atala.resolvers._
import scala.language.implicitConversions

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
      agentService <- ZIO.service[DidAgent]
      didCommService <- ZIO.service[DidOps]
      invitation = OutOfBandLoginInvitation(from = agentService.id)
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
      agentService <- ZIO.service[DidAgent]
      invitation = ConnectionInvitation.makeConnectionInvitation(from = agentService.id)
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
      _ <- Console.printLine("Read OutOfBand Invitation")
      data <- Console.readLine.flatMap {
        case ""  => ZIO.fail(???) // TODO retry
        case url => ZIO.succeed(Utils.parseLink(url).getOrElse(???)) /// TODO make ERROR
      }
      didCommService <- ZIO.service[DidOps]
      msg <- didCommService.unpack(data)
      outOfBandLoginInvitation = reaOutOfBandLoginInvitation(msg.message)
      agentService <- ZIO.service[DidAgent]
      reply = outOfBandLoginInvitation.reply(agentService.id)
      _ <- Console.printLine(s"Replying to ${outOfBandLoginInvitation.id} with $reply")

      res <- MessagingService.send(reply.makeMsg)
      _ <- Console.printLine(res.bodyAsString)
    } yield ()
  }

  def proposeAndSendCredential: ZIO[DidOps & DidAgent & DIDResolver & HttpClient, MercuryError | IOException, Unit] = {
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

      agentService <- ZIO.service[DidAgent]
      _ <- Console.printLine(s"Send to (ex: ${agentService.id})")
      sendTo <- Console.readLine.flatMap {
        case ""  => ZIO.succeed(agentService.id)
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
        from = agentService.id,
        to = sendTo,
      )
      _ <- Console.printLine(proposeCredential)
      msg = proposeCredential.makeMessage
      _ <- Console.printLine("Sending: " + msg)

      _ <- MessagingService.send(msg)
    } yield ()
  }

  def presentProof: ZIO[DidOps & DidAgent & DIDResolver & HttpClient, MercuryError | IOException, Unit] = {
    for {
      _ <- Console.printLine("Present Proof")
      agentService <- ZIO.service[DidAgent]

      _ <- Console.printLine(s"Request proof from did (ex: ${agentService.id})")
      requestTo <- Console.readLine.flatMap {
        case ""  => ZIO.succeed(agentService.id)
        case did => ZIO.succeed(DidId(did))
      }

      // Make a Request
      body = RequestPresentation.Body(goal_code = Some("Presentation Request"))
      presentationAttachmentAsJson = """{
                "challenge": "1f44d55f-f161-4938-a659-f8026467f126",
                "domain": "us.gov/DriverLicense",
                "credential_manifest": {}
            }"""

      attachmentDescriptor = AttachmentDescriptor.buildJsonAttachment(payload = presentationAttachmentAsJson)
      requestPresentation = RequestPresentation(
        body = body,
        attachments = Seq(attachmentDescriptor),
        to = requestTo,
        from = agentService.id,
      )
      msg = requestPresentation.makeMessage
      _ <- Console.printLine("Sending: " + msg)
      _ <- MessagingService.send(msg)
    } yield ()
  }

  def problemReport: ZIO[DidOps & DidAgent & DIDResolver & HttpClient, MercuryError | IOException, Unit] = {
    for {
      _ <- Console.printLine("Problem Report")
      agentService <- ZIO.service[DidAgent]

      _ <- Console.printLine(s"Problem report to did (ex: ${agentService.id})")
      requestTo <- Console.readLine.flatMap {
        case ""  => ZIO.succeed(agentService.id)
        case did => ZIO.succeed(DidId(did))
      }
      // Make a Problem Report
      reportproblem = ReportProblem.build(
        fromDID = agentService.id,
        toDID = requestTo,
        pthid = "1e513ad4-48c9-444e-9e7e-5b8b45c5e325",
        code = ProblemCode("e.p.xfer.cant-use-endpoint"),
        comment = Some("Unable to use the {1} endpoint for {2}.")
      )
      msg = reportproblem.toMessage
      _ <- Console.printLine("Sending: " + msg)
      _ <- MessagingService.send(msg)
    } yield ()
  }

  def connect: ZIO[DidOps & DidAgent & DIDResolver & HttpClient, MercuryError | IOException, Unit] = {

    import io.iohk.atala.mercury.protocol.invitation.OutOfBand
    import io.circe._, io.circe.parser._
    for {
      agentService <- ZIO.service[DidAgent]
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
        from = agentService.id,
        to = connectionInvitation.from,
        thid = None,
        pthid = Some(connectionInvitation.id),
        body = ConnectionRequest.Body(goal_code = Some("connect"), goal = Some("Establish Connection"))
      )
      msg = connectionRequest.makeMessage
      _ <- Console.printLine("Sending: " + msg)
      _ <- MessagingService.send(msg)
    } yield ()
  }

  private def webServer: App[DidOps & DidAgent & DIDResolver & HttpClient] = {
    val header = "content-type" -> MediaTypes.contentTypeEncrypted
    val (expectedKey, expectedValue) = header

    Http
      .collectZIO[Request] {
        case req @ Method.POST -> Root
            if req.rawHeader(expectedKey).fold(false) { _.equalsIgnoreCase(expectedValue) } =>
          val res = req.body.asString
            .catchNonFatalOrDie(ex => ZIO.fail(ParseResponse(ex)))
            .flatMap { data =>
              webServerProgram(data).catchAll { ex =>
                ZIO.fail(mercuryErrorAsThrowable(ex))
              }
            }
            .map(str => Response.text(str))

          res
        case Method.GET -> Root / "test" => ZIO.succeed(Response.text("Test ok!"))
        case req =>
          ZIO.logWarning(s"Received a not DID Comm v2 message: ${req}") *>
            ZIO.succeed(Response.text(s"The request must be a POST to root with the Header $header"))
      }
      .mapError(throwable => Response.fromHttpError(HttpError.InternalServerError(cause = Some(throwable))))
  }

  def startEndpoint: ZIO[DidOps & DidAgent & DIDResolver & HttpClient, IOException, Unit] = for {
    _ <- Console.printLine("Setup an endpoint")
    agentService <- ZIO.service[DidAgent]

    defaultPort = UniversalDidResolver
      .resolve(agentService.id.value)
      .get()
      .getDidCommServices()
      .asScala
      .toSeq
      .headOption
      .map(s => s.getServiceEndpoint())
      .flatMap(e => URL.decode(e).toOption)
      .flatMap(_.port)
      .getOrElse(8081) // default

    _ <- Console.printLine(s"Insert endpoint port ($defaultPort default) for (http://localhost:port)")
    port <- Console.readLine.flatMap {
      case ""  => ZIO.succeed(defaultPort)
      case str => ZIO.succeed(str.toIntOption.getOrElse(defaultPort))
    }
    server = {
      val config = Server.Config.default.copy(address = new java.net.InetSocketAddress(port))
      ZLayer.succeed(config) >>> Server.live
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
      case ""                               => ZIO.succeed(None) // default
      case str if str.toIntOption.isDefined => ZIO.succeed(str.toIntOption.map(port => s"http://localhost:$port"))
      case str                              => ZIO.succeed(Some(str))
    }

    didPeer <- for {
      peer <- ZIO.succeed(PeerDID.makePeerDid(serviceEndpoint = serviceEndpoint))
      // jwkForKeyAgreement <- ZIO.succeed(PeerDID.makeNewJwkKeyX25519)
      // jwkForKeyAuthentication <- ZIO.succeed(PeerDID.makeNewJwkKeyEd25519)
      // (jwkForKeyAgreement, jwkForKeyAuthentication)
      _ <- Console.printLine(s"New DID: ${peer.did}") *>
        Console.printLine(s"JWK for KeyAgreement: ${peer.jwkForKeyAgreement.toJSONString}") *>
        Console.printLine(s"JWK for KeyAuthentication: ${peer.jwkForKeyAuthentication.toJSONString}")
    } yield (peer)

    agentService = AgentPeerService.makeLayer(didPeer)

    layers: ZLayer[Any, Nothing, DidOps & DidAgent & DIDResolver & HttpClient] =
      DidCommX.liveLayer ++ agentService ++ DIDResolver.layer ++ ZioHttpClient.layer

    _ <- options(
      Seq(
        "none" -> ZIO.unit,
        "Show DID" -> Console.printLine(didPeer),
        "Get DID Document" -> Console.printLine("DID Document:") *> Console.printLine(didPeer.getDIDDocument),
        "Start WebServer endpoint" -> startEndpoint.provide(layers),
        "Ask for Mediation Coordinate" -> askForMediation.provide(layers),
        "Generate login invitation" -> generateLoginInvitation.provide(DidCommX.liveLayer ++ agentService),
        "Login with DID" -> loginInvitation.provide(layers),
        "Propose Credential" -> proposeAndSendCredential.provide(layers),
        "Present Proof" -> presentProof.provide(layers),
        "Generate Connection invitation" -> generateConnectionInvitation.provide(DidCommX.liveLayer ++ agentService),
        "Connect" -> connect.provide(layers),
        "Problem Report" -> problemReport.provide(layers),
      )
    ).repeatWhile((_) => true)

  } yield ()

  def webServerProgram(
      jsonString: String
  ): ZIO[DidOps & DidAgent & DIDResolver & HttpClient, MercuryThrowable, String] = { // TODO Throwable
    import io.iohk.atala.mercury.DidOps.*
    ZIO.logAnnotate("request-id", java.util.UUID.randomUUID.toString()) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(jsonString)
        msg <- unpack(jsonString).map(_.message)
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

                didCommService <- ZIO.service[DidOps]
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

                didCommService <- ZIO.service[DidOps]
                msgToSend = requestCredential.makeMessage
                _ <- MessagingService.send(msgToSend)
              } yield ("RequestCredential Sent")

            case s if s == RequestCredential.`type` => // Issuer
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Issuer in issue-credential:")
                _ <- ZIO.logInfo("Got RequestCredential: " + msg)
                issueCredential = IssueCredential.makeIssueCredentialFromRequestCredential(msg) // IssueCredential

                didCommService <- ZIO.service[DidOps]
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
            // ########################
            // ### Present-Proof ###
            // ########################
            case s if s == RequestPresentation.`type` => // Prover
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Prover in Present-Proof:")
                requestPresentation = RequestPresentation.readFromMessage(msg)
                _ <- ZIO.logInfo("Got RequestPresentation: " + requestPresentation)
                presentation = Presentation.makePresentationFromRequest(msg)
                didCommService <- ZIO.service[DidOps]
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
            // ########################
            // ### ReportProblem ###
            // ########################
            case s if s == ReportProblem.`type` => // receiver
              for {
                _ <- ZIO.logInfo("Received Problem report")
                reportProblem = ReportProblem.readFromMessage(msg)
                _ <- ZIO.logInfo("Got Problemreport: " + reportProblem)
              } yield ("Problemreport Recived")
            // ########################
            // ### Comnnect ###
            // ########################
            case s if s == ConnectionRequest.`type` => // Inviter
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As Inviter in Connect:")
                connectionRequest = ConnectionRequest.fromMessage(msg).toOption.get // TODO .get
                _ <- ZIO.logInfo("Got ConnectionRequest: " + connectionRequest)
                _ <- ZIO.logInfo("Creating New PeerDID...")
                //                peer <- ZIO.succeed(PeerDID.makePeerDid(serviceEndpoint = serviceEndpoint)) TODO
                //                _ <- ZIO.logInfo(s"My new DID => $peer")
                connectionResponse = ConnectionResponse.makeResponseFromRequest(msg).toOption.get // TODO .get
                msgToSend = connectionResponse.makeMessage
                _ <- MessagingService.send(msgToSend)
              } yield ("Connection Request Sent")
            case s if s == ConnectionResponse.`type` => // Invitee
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As Invitee in Connect:")
                connectionResponse = ConnectionResponse.fromMessage(msg).toOption.get // TODO .get
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
