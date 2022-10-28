package io.iohk.atala.mercury

import scala.jdk.CollectionConverters.*

import zio._
import java.io.IOException
import io.iohk.atala.resolvers.PeerDidMediatorSecretResolver
import zhttp.service.ChannelFactory
import zhttp.service.EventLoopGroup
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.resolvers.UniversalDidResolver
import io.iohk.atala.resolvers.CharlieSecretResolver
import zhttp.service._
import zhttp.http._
import io.iohk.atala.QRcode
import io.iohk.atala.mercury.model.{_, given}
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.mercury.protocol.outofbandlogin._
import io.iohk.atala.mercury.protocol.issuecredential._

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
      _ <- ZIO.foreach(p.zipWithIndex)(e => Console.printLine(e._2 + " - " + e._1._1))
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

  val env = ChannelFactory.auto ++ EventLoopGroup.auto()

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
        .provideSomeLayer(env)
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

      encryptedForwardMessage <- didCommService.packEncrypted(reply.makeMsg, to = reply.to)
      jsonString = encryptedForwardMessage.string

      serviceEndpoint = UniversalDidResolver
        .resolve(reply.to.value)
        .get()
        .getDidCommServices()
        .asScala
        .toSeq
        .headOption
        .map(s => s.getServiceEndpoint())

      _ <- Console.printLine("Sending to" + serviceEndpoint)
      res <- Client
        .request(
          url = serviceEndpoint.get, // TODO make ERROR type
          method = Method.POST,
          headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
          content = Body.fromChunk(Chunk.fromArray(jsonString.getBytes)),
          // ssl = ClientSSLOptions.DefaultSSL,
        )
        .provideSomeLayer(env)
      data <- res.body.asString
      _ <- Console.printLine(data)
    } yield ()
  }

  def proposeAndSendCredential: ZIO[DidComm, MercuryError | IOException, Unit] = {
    for {

      _ <- Console.printLine("Propose Credential")
      _ <- Console.printLine("What is the the Playload")
      playloadData <- Console.readLine.flatMap {
        case ""   => ZIO.succeed("playload")
        case data => ZIO.succeed(data)
      }

      attachmentDescriptor =
        AttachmentDescriptor.buildAttachment(payload = playloadData)
      attribute1 = Attribute(name = "name", value = "Joe Blog")
      attribute2 = Attribute(name = "dob", value = "01/10/1947")
      credentialPreview = CredentialPreview(attributes = Seq(attribute1, attribute2))

      proposeCredential = ProposeCredential(
        body = ProposeCredential.Body(
          goal_code = Some("goal_code"),
          comment = None,
          credential_preview = credentialPreview, // Option[CredentialPreview], // JSON STRinf
          formats = Seq.empty // : Seq[CredentialFormat]
        ),
        attachments = Seq(attachmentDescriptor)
      )
      _ <- Console.printLine("What is the the Playload")
      _ <- Console.printLine(proposeCredential)

      didCommService <- ZIO.service[DidComm]
      _ <- Console.printLine(s"Send to (ex: ${didCommService.myDid})")
      didStr <- Console.readLine.flatMap {
        case ""  => ZIO.succeed(didCommService.myDid)
        case did => ZIO.succeed(DidId(did))
      }
      sendTo = didStr
      msg = proposeCredential.makeMessage(from = didCommService.myDid, sendTo = sendTo)
      _ <- Console.printLine("Sending: " + msg)

      _ <- sendMessage(msg)
    } yield ()
  }

  /** Encrypt and send a Message via HTTP
    *
    * TODO Move this method to another model
    */
  def sendMessage(msg: Message): ZIO[DidComm, MercuryException, Unit] = { // TODO  Throwable
    for {
      didCommService <- ZIO.service[DidComm]

      encryptedForwardMessage <- didCommService.packEncrypted(msg, to = msg.to.get)
      jsonString = encryptedForwardMessage.string

      serviceEndpoint = UniversalDidResolver
        .resolve(msg.to.get.value) // TODO GET
        .get()
        .getDidCommServices()
        .asScala
        .toSeq
        .headOption
        .map(s => s.getServiceEndpoint())
        .get // TODO make ERROR type

      _ <- Console.printLine("Sending to" + serviceEndpoint)

      res <- Client
        .request(
          url = serviceEndpoint,
          method = Method.POST,
          headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
          content = Body.fromChunk(Chunk.fromArray(jsonString.getBytes)),
          // ssl = ClientSSLOptions.DefaultSSL,
        )
        .provideSomeLayer(env)
        .catchNonFatalOrDie { ex => ZIO.fail(SendMessage(ex)) }
      data <- res.body.asString
        .catchNonFatalOrDie { ex => ZIO.fail(ParseResponse(ex)) }
      _ <- Console.printLine(data)
    } yield ()
  }

  def webServer(port: Int): HttpApp[DidComm, Throwable] = {
    val header = "content-type" -> MediaTypes.contentTypeEncrypted
    Http
      .collectZIO[Request] {
        case req @ Method.POST -> !!
            if req.headersAsList.exists(h => h._1.equalsIgnoreCase(header._1) && h._2.equalsIgnoreCase(header._2)) =>
          req.body.asString
            .catchNonFatalOrDie(ex => ZIO.fail(ParseResponse(ex)))
            .flatMap { data =>
              webServerProgram(data).catchAll { ex =>
                ZIO.fail(mercuryErrorAsThrowable(ex))
              }
            }
            .map(str => Response.text(str))
        case Method.GET -> !! / "test" => ZIO.succeed(Response.text("Test ok!"))
        case req => ZIO.succeed(Response.text(s"The request must be a POST to root with the Header $header"))
      }

  }

  def startEndpoint = for {
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
    _ <- Server.start(port, webServer(port)).fork
    _ <- Console.printLine("Endpoint Started")
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

    _ <- options(
      Seq(
        "none" -> ZIO.unit,
        "Show DID" -> Console.printLine(agentDID),
        "Get DID Document" -> Console.printLine("DID Document:") *> Console.printLine(agentDID.getDIDDocument),
        "Start WebServer endpoint" -> startEndpoint.provide(didCommLayer),
        "Ask for Mediation Coordinate" -> askForMediation.provide(didCommLayer),
        "Generate login invitation" -> generateLoginInvitation.provide(didCommLayer),
        "Login with DID" -> loginInvitation.provide(didCommLayer),
        "Propose Credential" -> proposeAndSendCredential.provide(didCommLayer),
      )
    ).repeatWhile((_) => true)

  } yield ()

  def webServerProgram(
      jsonString: String
  ): ZIO[DidComm, MercuryThrowable, String] = { // TODO Throwable
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
                msg = offer.makeMessage(from = didCommService.myDid)
                _ <- sendMessage(msg)
              } yield ("OfferCredential Sent")

            case s if s == OfferCredential.`type` => // Holder
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Holder in issue-credential:")
                _ <- ZIO.logInfo("Got OfferCredential: " + msg)
                // store on BD TODO //pc = OfferCredential.readFromMessage(msg)
                requestCredential = RequestCredential.makeRequestCredentialFromOffer(msg) // RequestCredential

                didCommService <- ZIO.service[DidComm]
                msg = requestCredential.makeMessage(from = didCommService.myDid)
                _ <- sendMessage(msg)
              } yield ("RequestCredential Sent")

            case s if s == RequestCredential.`type` => // Issuer
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Issuer in issue-credential:")
                _ <- ZIO.logInfo("Got RequestCredential: " + msg)
                issueCredential = IssueCredential.makeIssueCredentialFromRequestCredential(msg) // IssueCredential

                didCommService <- ZIO.service[DidComm]
                msg = issueCredential.makeMessage(from = didCommService.myDid)
                _ <- sendMessage(msg)
              } yield ("IssueCredential Sent")

            case s if s == IssueCredential.`type` => // Holder
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Holder in issue-credential:")
                _ <- ZIO.logInfo("Got IssueCredential: " + msg)
              } yield ("IssueCredential Received")

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
