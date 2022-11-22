package io.iohk.atala.agent.server

import zio.*
import io.iohk.atala.mercury._
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.resolvers.UniversalDidResolver
import io.iohk.atala.castor.sql.repository.{Migrations => CastorMigrations}
import io.iohk.atala.pollux.sql.repository.{Migrations => PolluxMigrations}

object Main extends ZIOAppDefault {
  def agentLayer(peer: PeerDID): ZLayer[Any, Nothing, AgentServiceAny] = ZLayer.succeed(
    io.iohk.atala.mercury.AgentServiceAny(
      new DIDComm(UniversalDidResolver, peer.getSecretResolverInMemory),
      peer.did
    )
  )

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

      // Execute migrations from Castor and Pollux libraries using Flyway
      _ <- ZIO
        .serviceWithZIO[CastorMigrations](_.migrate)
        .provide(RepoModule.castorDbConfigLayer >>> CastorMigrations.layer)
      _ <- ZIO
        .serviceWithZIO[PolluxMigrations](_.migrate)
        .provide(RepoModule.polluxDbConfigLayer >>> PolluxMigrations.layer)

      agentDID <- for {
        peer <- ZIO.succeed(PeerDID.makePeerDid(serviceEndpoint = Some(s"http://localhost:$didCommServicePort")))
        _ <- ZIO.logInfo(s"New DID: ${peer.did}") *>
          ZIO.logInfo(s"JWK for KeyAgreement: ${peer.jwkForKeyAgreement.toJSONString}") *>
          ZIO.logInfo(s"JWK for KeyAuthentication: ${peer.jwkForKeyAuthentication.toJSONString}")
      } yield (peer)

      didCommLayer = agentLayer(agentDID)

      didCommExchangesFiber <- Modules.didCommExchangesJob
        .provide(didCommLayer)
        .debug
        .fork

      didCommServiceFiber <- Modules
        .didCommServiceEndpoint(didCommServicePort)
        .provide(didCommLayer, AppModule.credentialServiceLayer)
        .debug
        .fork

      _ <- Modules.app(restServicePort).provide(didCommLayer)
      _ <- Modules.zioApp
      _ <- ZIO.never
    } yield ()

}
