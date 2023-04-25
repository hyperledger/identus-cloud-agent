package io.iohk.atala.agent.server

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import zio.*
import io.iohk.atala.mercury.*
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.resolvers.UniversalDidResolver
import io.iohk.atala.castor.sql.repository.Migrations as CastorMigrations
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
import zhttp.http.*
import zhttp.service.Server
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

import java.security.Security

object SystemInfoApp extends ZIOAppDefault {
  private val metricsConfig = ZLayer.succeed(MetricsConfig(5.seconds))

  def run =
    for {
      systemServicePort <- System.env("SYSTEM_SERVICE_PORT").map {
        case Some(s) if s.toIntOption.isDefined => s.toInt
        case _                                  => 8082
      }
      _ <- Server
        .start(
          port = systemServicePort,
          http = Http.collectZIO[Request] {
            case Method.GET -> !! / "metrics" =>
              ZIO.serviceWithZIO[PrometheusPublisher](_.get.map(Response.text))
            case Method.GET -> !! / "health" =>
              ZIO
                .succeed(
                  Response.json(
                    HealthInfo(
                      version = BuildInfo.version
                    ).asJson.toString
                  )
                )
          }
        )
        .provide(
          metricsConfig,
          prometheus.publisherLayer,
          prometheus.prometheusLayer
        )
    } yield ()

}

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

  def appComponents(didCommServicePort: Int, restServicePort: Int) = for {
    _ <- Modules.issueCredentialDidCommExchangesJob.debug.fork
    _ <- Modules.presentProofExchangeJob.debug.fork
    _ <- Modules.connectDidCommExchangesJob.debug.fork
    _ <- Modules.didCommServiceEndpoint(didCommServicePort).debug.fork
    _ <- Modules.syncDIDPublicationStateFromDltJob.fork
    _ <- Modules.app(restServicePort).fork
    _ <- Modules.zioApp.debug.fork
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
        IssueControllerImpl.layer
      )
    } yield app

    app.provide(
      RepoModule.polluxDbConfigLayer >>> PolluxMigrations.layer,
      RepoModule.connectDbConfigLayer >>> ConnectMigrations.layer,
      RepoModule.agentDbConfigLayer >>> AgentMigrations.layer,
    )
  }

}

object MainApp extends ZIOApp.Proxy(SystemInfoApp <> DefaultJvmMetrics.app <> AgentApp)
