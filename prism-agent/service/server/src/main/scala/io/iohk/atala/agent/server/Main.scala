package io.iohk.atala.agent.server

import zio.*
import io.iohk.atala.mercury.PeerDID
import io.iohk.atala.mercury.AgentCli

object Main extends ZIOAppDefault {
  override def run: ZIO[Any, Throwable, Unit] =
    for {
      _ <- Console
        .printLine("""
      |██████╗ ██████╗ ██╗███████╗███╗   ███╗
      |██╔══██╗██╔══██╗██║██╔════╝████╗ ████║
      |██████╔╝██████╔╝██║███████╗██╔████╔██║
      |██╔═══╝ ██╔══██╗██║╚════██║██║╚██╔╝██║
      |██║     ██║  ██║██║███████║██║ ╚═╝ ██║
      |╚═╝     ╚═╝  ╚═╝╚═╝╚══════╝╚═╝     ╚═╝
      |
      | █████╗  ██████╗ ███████╗███╗   ██╗████████╗
      |██╔══██╗██╔════╝ ██╔════╝████╗  ██║╚══██╔══╝
      |███████║██║  ███╗█████╗  ██╔██╗ ██║   ██║
      |██╔══██║██║   ██║██╔══╝  ██║╚██╗██║   ██║
      |██║  ██║╚██████╔╝███████╗██║ ╚████║   ██║
      |╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═══╝   ╚═╝
      |""".stripMargin)
        .ignore

      serviceEndpointPort <- System.env("DIDCOMM_ENDPOINT_PORT").map {
        case Some(s) if s.toIntOption.isDefined => s.toInt
        case _                                  => 8090
      }

      _ <- Console.printLine(s"DIDComm Service Endpoint port => $serviceEndpointPort")

      agentDID <- for {
        peer <- ZIO.succeed(PeerDID.makePeerDid(serviceEndpoint = Some(s"http://localhost:$serviceEndpointPort")))
        _ <- Console.printLine(s"New DID: ${peer.did}") *>
          Console.printLine(s"JWK for KeyAgreement: ${peer.jwkForKeyAgreement.toJSONString}") *>
          Console.printLine(s"JWK for KeyAuthentication: ${peer.jwkForKeyAuthentication.toJSONString}")
      } yield (peer)

      didCommLayer = AgentCli.agentLayer(agentDID)

      _ <- Modules.didCommExchangesJob
        .provide(
          didCommLayer
        )
        .fork
      _ <- Modules
        .didCommServiceEndpoint(serviceEndpointPort)
        .provide(
          didCommLayer,
          AppModule.credentialServiceLayer
        )
        .fork
      _ <- Modules.app
    } yield ()

}
