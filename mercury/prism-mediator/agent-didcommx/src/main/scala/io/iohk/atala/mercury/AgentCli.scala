package io.iohk.atala.mercury

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
import io.iohk.atala.mercury.model.Message
import io.iohk.atala.mercury.model.DidId

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

  def options(p: Seq[(String, ZIO[Any, Throwable, Unit])]): ZIO[Any, Throwable, Unit] = {
    for {
      _ <- Console.printLine("--- Choose an option: ---")
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

    def makeMsg(i: OutOfBandLoginInvitation): Message = Message(
      piuri = i.`type`,
      id = i.id,
      from = Some(i.from): Option[DidId],
      to = None,
    )

    // InvitationPrograms.createInvitationV2().map(oob => Response.text(serverUrl + oob))
    for {
      didCommService <- ZIO.service[DidComm]
      invitation = OutOfBandLoginInvitation(from = didCommService.myDid)
      invitationSigned <- didCommService.packSigned(makeMsg(invitation))
      serverUrl = s"http://locahost:${9999}?_oob=${invitationSigned.base64}" // FIXME locahost
      _ <- Console.printLine(QRcode.getQr(serverUrl).toString)
      _ <- Console.printLine(serverUrl)
      _ <- Console.printLine(invitation.id + " -> " + invitation)
    } yield ()
  }

  def loginInvitation = {
    import io.iohk.atala.mercury.protocol.outofbandlogin._

    def reaOutOfBandLoginInvitation(msg: org.didcommx.didcomm.message.Message): OutOfBandLoginInvitation =
      // OutOfBandLoginInvitation(`type` = msg.piuri, id = msg.id, from = msg.from.get)
      OutOfBandLoginInvitation(`type` = msg.getType(), id = msg.getId(), from = DidId(msg.getFrom()))

    // InvitationPrograms.createInvitationV2().map(oob => Response.text(serverUrl + oob))
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
    } yield ()
  }

  def app(port: Int): HttpApp[DidComm, Throwable] = {
    val header = "content-type" -> MediaTypes.contentTypeEncrypted
    Http.collectZIO[Request] {
      case req @ Method.POST -> !!
          if req.headersAsList.exists(h => h._1.equalsIgnoreCase(header._1) && h._2.equalsIgnoreCase(header._2)) =>
        req.bodyAsString
          // .flatMap(data => MediatorProgram.program(data))
          .map(str => Response.text(str))
      case Method.GET -> !! / "test" => ZIO.succeed(Response.text("Test ok!"))
      case req => ZIO.succeed(Response.text(s"The request must be a POST to root with the Header $header"))
    }
  }

  def startEndpoint = for {
    _ <- Console.printLine("Setup a endpoint")
    defualtPort = 8001 // defualt
    _ <- Console.printLine(s"Inserte endpoint port ($defualtPort defualt) for (http://localhost:port)")
    port <- Console.readLine.flatMap {
      case ""  => ZIO.succeed(defualtPort)
      case str => ZIO.succeed(str.toIntOption.getOrElse(defualtPort))
    }
    _ <- Server.start(port, app(port)).fork
    _ <- Console.printLine("Endpoint Started")
  } yield ()

  def run = for {
    _ <- startLogo
    // makeNewDID <- questionYN("Generate new 'peer' DID?")
    _ <- Console.printLine("Generating a new 'peer' DID!")
    // haveServiceEndpoint <- questionYN("Do you have a serviceEndpoint url? e.g http://localhost:8080/myendpoint")
    // ZIO.when(haveServiceEndpoint)( // )
    _ <- Console.printLine("Enter the serviceEndpoint URL (defualt None)")
    serviceEndpoint <- Console.readLine.flatMap {
      case ""  => ZIO.succeed(None) // defualt
      case str => ZIO.succeed(Some(str))
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
        "Show DID" -> Console.printLine(agentDID),
        "Get DID Document" ->
          Console.printLine("DID Document:") *>
          Console.printLine(agentDID.getDIDDocument),
        "Ask for Mediation Coordinate" -> askForMediation.provide(didCommLayer),
        "Start a endpoint" -> startEndpoint.provide(didCommLayer),
        "Generate login invitation" -> generateLoginInvitation.provide(didCommLayer),
        "Login with DID" -> loginInvitation.provide(didCommLayer),
      )
    ).repeatN(10)

  } yield ()

}
