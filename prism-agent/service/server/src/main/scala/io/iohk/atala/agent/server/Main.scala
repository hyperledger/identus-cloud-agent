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

      restServicePort <- System.env("REST_SERVICE_PORT").map {
        case Some(s) if s.toIntOption.isDefined => s.toInt
        case _                                  => 8080
      }
      _ <- ZIO.logInfo(s"REST Service port => $restServicePort")

      didCommServicePort <- System.env("DIDCOMM_SERVICE_PORT").map {
        case Some(s) if s.toIntOption.isDefined => s.toInt
        case _                                  => 8090
      }
      _ <- ZIO.logInfo(s"DIDComm Service port => $didCommServicePort")

      agentDID <- for {
        peer <- ZIO.succeed(PeerDID.makePeerDid(serviceEndpoint = Some(s"http://localhost:$didCommServicePort")))
        _ <- ZIO.logInfo(s"New DID: ${peer.did}") *>
          ZIO.logInfo(s"JWK for KeyAgreement: ${peer.jwkForKeyAgreement.toJSONString}") *>
          ZIO.logInfo(s"JWK for KeyAuthentication: ${peer.jwkForKeyAuthentication.toJSONString}")
      } yield (peer)

      didCommLayer = AgentCli.agentLayer(agentDID)

      didCommExchangesFiber <- Modules.didCommExchangesJob
        .provide(
          didCommLayer
        )
        .debug
        .fork

      didCommServiceFiber <- Modules
        .didCommServiceEndpoint(didCommServicePort)
        .provide(
          didCommLayer,
          AppModule.credentialServiceLayer
        )
        .debug
        .fork

      _ <- Modules.app(restServicePort).provide(didCommLayer)
    } yield ()

}
