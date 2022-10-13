package io.iohk.atala.mercury

import zio._
import java.io.IOException
import io.iohk.atala.resolvers.PeerDidMediatorSecretResolver
import zhttp.service.ChannelFactory
import zhttp.service.EventLoopGroup
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.resolvers.UniversalDidResolver
import io.iohk.atala.resolvers.CharlieSecretResolver

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

  def options(p: Map[String, ZIO[Any, Throwable, Unit]]): ZIO[Any, Throwable, Unit] = {
    for {
      _ <- Console.printLine("--- Choose an option: ---")
      _ <- ZIO.foreach(p.zipWithIndex)(e => Console.printLine(e._2 + " - " + e._1._1))
      _ <- Console.readLine.flatMap { e => p.values.toSeq(e.toInt) }
    } yield ()
  }

  // val startLogo = Console.printLine("""
  //   |    █████╗  ██████╗ ███████╗███╗   ██╗████████╗       ██████╗██╗     ██╗
  //   |   ██╔══██╗██╔════╝ ██╔════╝████╗  ██║╚══██╔══╝      ██╔════╝██║     ██║
  //   |   ███████║██║  ███╗█████╗  ██╔██╗ ██║   ██║   █████╗██║     ██║     ██║
  //   |   ██╔══██║██║   ██║██╔══╝  ██║╚██╗██║   ██║   ╚════╝██║     ██║     ██║
  //   |   ██║  ██║╚██████╔╝███████╗██║ ╚████║   ██║         ╚██████╗███████╗██║
  //   |   ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═══╝   ╚═╝          ╚═════╝╚══════╝╚═╝
  //   |""".stripMargin)

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

  def askForMediation(peer: PeerDID) = {

    val agentLayer = ZLayer.succeed(
      io.iohk.atala.mercury.AgentServiceAny(
        new DIDComm(UniversalDidResolver, peer.getSecretResolverInMemory),
        peer.did
      )
    )

    for {
      _ <- Console.printLine("Enter the Mediator URL (defualt is 'http://localhost:8000')")
      url <- Console.readLine.flatMap {
        case ""  => ZIO.succeed("http://localhost:8000") // defualt
        case str => ZIO.succeed(str)
      }
      _ <- CoordinateMediationPrograms
        .senderMediationRequestProgram(mediatorURL = url)
        .provide(env, agentLayer)
    } yield ()
  }

  def run = for {
    _ <- startLogo
    makeNewDID <- questionYN("Generate new 'peer' DID?")
    haveServiceEndpoint <- questionYN("Do you have a serviceEndpoint url? e.g http://localhost:8080/myendpoint")
    _ <- ZIO.when(haveServiceEndpoint)(Console.printLine("Enter the serviceEndpoint URL"))
    serviceEndpoint <- ZIO.when(haveServiceEndpoint) {
      Console.readLine.flatMap {
        case ""  => ZIO.succeed(None) // defualt
        case str => ZIO.succeed(Some(str))
      }
    }

    agentDID <-
      // if (makeNewDID)  //FIXME
      for {
        _ <- Console.printLine(s"New DID: ${makeNewDID}")
        _ <- Console.printLine(s"New DID: ${haveServiceEndpoint}")
        _ <- Console.printLine(s"New DID: ${serviceEndpoint.flatten}")
        peer <- ZIO.succeed(PeerDID.makePeerDid(serviceEndpoint = serviceEndpoint.flatten))
        // jwkForKeyAgreement <- ZIO.succeed(PeerDID.makeNewJwkKeyX25519)
        // jwkForKeyAuthentication <- ZIO.succeed(PeerDID.makeNewJwkKeyEd25519)
        // (jwkForKeyAgreement, jwkForKeyAuthentication)
        _ <- Console.printLine(s"New DID: ${peer.did}") *>
          Console.printLine(s"JWK for KeyAgreement: ${peer.jwkForKeyAgreement.toJSONString}") *>
          Console.printLine(s"JWK for KeyAuthentication: ${peer.jwkForKeyAuthentication.toJSONString}")
      } yield (peer)

    _ <- options(
      Map(
        "Show DID" -> Console.printLine(agentDID),
        "Get DID Document" ->
          Console.printLine("DID Document:") *>
          Console.printLine(agentDID.getDIDDocument),
        "Ask for Mediation Coordinate" -> askForMediation(agentDID)
      )
    ).repeatN(10)

  } yield ()

}
