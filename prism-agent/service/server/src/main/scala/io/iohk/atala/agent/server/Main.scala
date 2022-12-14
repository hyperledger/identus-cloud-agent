package io.iohk.atala.agent.server

import zio.*
import io.iohk.atala.mercury._
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.resolvers.UniversalDidResolver
import io.iohk.atala.castor.sql.repository.{Migrations => CastorMigrations}
import io.iohk.atala.pollux.sql.repository.{Migrations => PolluxMigrations}
import io.iohk.atala.connect.sql.repository.{Migrations => ConnectMigrations}
import io.iohk.atala.agent.server.sql.{Migrations => AgentMigrations}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.resolvers.DIDResolver
import io.iohk.atala.agent.server.http.ZioHttpClient
import org.flywaydb.core.extensibility.AppliedMigration
import io.iohk.atala.pollux.service.SchemaRegistryServiceInMemory
import io.iohk.atala.pollux.service.VerificationPolicyServiceInMemory

object Main extends ZIOAppDefault {

  def didCommAgentLayer(didCommServiceUrl: String) = ZLayer(
    for {
      managedDIDService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDIDService.createAndStorePeerDID(didCommServiceUrl)
      _ <- ZIO.logInfo(s"New DID: ${peerDID.did}")
    } yield io.iohk.atala.mercury.AgentServiceAny(
      new DIDComm(UniversalDidResolver, peerDID.getSecretResolverInMemory),
      peerDID.did
    )
  )

  val migrations = for {
    _ <- ZIO.serviceWithZIO[PolluxMigrations](_.migrate)
    _ <- ZIO.serviceWithZIO[ConnectMigrations](_.migrate)
    _ <- ZIO.serviceWithZIO[AgentMigrations](_.migrate)
  } yield ()

  def appComponents(didCommServicePort: Int, restServicePort: Int) = for {
    _ <- Modules.didCommExchangesJob.debug.fork
    _ <- Modules.presentProofExchangeJob.debug.fork
    _ <- Modules.connectDidCommExchangesJob.debug.fork
    _ <- Modules.didCommServiceEndpoint(didCommServicePort).debug.fork
    _ <- Modules.app(restServicePort).fork
    _ <- Modules.zioApp.fork
    _ <- ZIO.never
  } yield ()

  override def run: ZIO[Any, Throwable, Unit] = {
    val app = for {
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

      didCommServiceUrl <- System.env("DIDCOMM_SERVICE_URL").map {
        case Some(s) => s
        case _       => "http://localhost"
      }
      _ <- ZIO.logInfo(s"DIDComm Service URL => $didCommServiceUrl")

      didCommServicePort <- System.env("DIDCOMM_SERVICE_PORT").map {
        case Some(s) if s.toIntOption.isDefined => s.toInt
        case _                                  => 8090
      }
      _ <- ZIO.logInfo(s"DIDComm Service port => $didCommServicePort")

      _ <- migrations

      app <- appComponents(didCommServicePort, restServicePort).provide(
        didCommAgentLayer(didCommServiceUrl),
        DIDResolver.layer,
        ZioHttpClient.layer,
        AppModule.credentialServiceLayer,
        AppModule.presentationServiceLayer,
        AppModule.connectionServiceLayer,
        SystemModule.configLayer,
        SystemModule.actorSystemLayer,
        HttpModule.layers,
        SchemaRegistryServiceInMemory.layer,
        VerificationPolicyServiceInMemory.layer,
        AppModule.manageDIDServiceLayer
      )
    } yield app

    app.provide(
      RepoModule.polluxDbConfigLayer >>> PolluxMigrations.layer,
      RepoModule.connectDbConfigLayer >>> ConnectMigrations.layer,
      RepoModule.agentDbConfigLayer >>> AgentMigrations.layer,
    )
  }

}
