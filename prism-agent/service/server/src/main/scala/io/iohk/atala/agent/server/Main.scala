package io.iohk.atala.agent.server

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import zio.*
import io.iohk.atala.mercury.*
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.resolvers.UniversalDidResolver
import io.iohk.atala.pollux.sql.repository.Migrations as PolluxMigrations
import io.iohk.atala.connect.sql.repository.Migrations as ConnectMigrations
import io.iohk.atala.agent.server.sql.Migrations as AgentMigrations
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.resolvers.DIDResolver
import io.iohk.atala.agent.server.http.ZioHttpClient
import org.flywaydb.core.extensibility.AppliedMigration
import io.iohk.atala.pollux.core.service.CredentialSchemaServiceImpl
import io.iohk.atala.pollux.sql.repository.JdbcCredentialSchemaRepository
import io.iohk.atala.agent.walletapi.sql.JdbcDIDSecretStorage
import zio.http.*
import zio.http.model.*
import zio.http.ZClient.ClientLive
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.{MetricsConfig, prometheus}
import zio.metrics.jvm.DefaultJvmMetrics
import io.iohk.atala.agent.server.buildinfo.BuildInfo
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import io.iohk.atala.agent.server.health.HealthInfo
import io.iohk.atala.connect.controller.ConnectionControllerImpl
import io.iohk.atala.issue.controller.IssueControllerImpl
import io.iohk.atala.castor.controller.DIDControllerImpl
import io.iohk.atala.castor.controller.DIDRegistrarControllerImpl

import java.security.Security
import io.iohk.atala.agent.server.http.HttpRoutes
import io.iohk.atala.presentproof.controller.PresentProofControllerImpl

object AgentApp extends ZIOAppDefault {

  Security.insertProviderAt(BouncyCastleProviderSingleton.getInstance(), 2)

  def didCommAgentLayer(didCommServiceUrl: String): ZLayer[ManagedDIDService, Nothing, DidAgent] = {
    val aux = for {
      managedDIDService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDIDService.createAndStorePeerDID(didCommServiceUrl)
      _ <- ZIO.logInfo(s"New DID: ${peerDID.did}")
    } yield io.iohk.atala.mercury.AgentPeerService.makeLayer(peerDID)
    ZLayer.fromZIO(aux).flatten
  }

  val migrations = for {
    _ <- ZIO.serviceWithZIO[PolluxMigrations](_.migrate)
    _ <- ZIO.serviceWithZIO[ConnectMigrations](_.migrate)
    _ <- ZIO.serviceWithZIO[AgentMigrations](_.migrate)
  } yield ()

  def serverProgram(didCommServicePort: Int) = {
    val server = {
      val config = ServerConfig(address = new java.net.InetSocketAddress(didCommServicePort))
      ServerConfig.live(config)(using Trace.empty) >>> Server.live
    }
    for {
      _ <- ZIO.logInfo(s"Server Started on port $didCommServicePort")
      myServer <- {
        Server
          .serve(Modules.didCommServiceEndpoint ++ SystemInfoApp.app)
          .provideSomeLayer(server)
          .debug *> ZIO.logWarning(s"Server STOP (on port $didCommServicePort)")
      }.fork
    } yield (myServer)
  }

  def appComponents(didCommServicePort: Int, restServicePort: Int) = for {
    _ <- Modules.issueCredentialDidCommExchangesJob.debug.fork
    _ <- Modules.presentProofExchangeJob.debug.fork
    _ <- Modules.connectDidCommExchangesJob.debug.fork
    server <- serverProgram(didCommServicePort)
    _ <- Modules.syncDIDPublicationStateFromDltJob.fork
    _ <- Modules.app(restServicePort).fork
    _ <- Modules.zioApp.fork
    _ <- server.join *> ZIO.log(s"Server End")
    _ <- ZIO.never
  } yield ()

  override def run: ZIO[Any, Throwable, Unit] = {

    val app = for {
      _ <- Console
        .printLine(s"""
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
      |
      |version: ${BuildInfo.version}
      |
      |""".stripMargin)
        .ignore

      restServicePort <- System.env("REST_SERVICE_PORT").map {
        case Some(s) if s.toIntOption.isDefined => s.toInt
        case _                                  => 8080
      }
      _ <- ZIO.logInfo(s"REST Service port => $restServicePort")

      didCommServiceUrl <- System.env("DIDCOMM_SERVICE_URL").map {
        case Some(s) => s
        case _       => "http://localhost:8090"
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
        DidCommX.liveLayer,
        AppModule.didJwtResolverlayer,
        AppModule.didServiceLayer,
        DIDResolver.layer,
        ZioHttpClient.layer,
        AppModule.credentialServiceLayer,
        AppModule.presentationServiceLayer,
        AppModule.connectionServiceLayer,
        SystemModule.configLayer,
        SystemModule.actorSystemLayer,
        HttpModule.layers,
        RepoModule.credentialSchemaServiceLayer,
        AppModule.manageDIDServiceLayer,
        RepoModule.verificationPolicyServiceLayer,
        ConnectionControllerImpl.layer,
        DIDControllerImpl.layer,
        IssueControllerImpl.layer,
        DIDRegistrarControllerImpl.layer,
        PresentProofControllerImpl.layer
      )
    } yield app

    app.provide(
      RepoModule.polluxDbConfigLayer >>> PolluxMigrations.layer,
      RepoModule.connectDbConfigLayer >>> ConnectMigrations.layer,
      RepoModule.agentDbConfigLayer >>> AgentMigrations.layer,
    )
  }

}

object MainApp extends ZIOApp.Proxy(DefaultJvmMetrics.app <> AgentApp)
