package io.iohk.atala.agent.server

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import io.iohk.atala.agent.server.http.ZioHttpClient
import io.iohk.atala.agent.server.sql.Migrations as AgentMigrations
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.agent.walletapi.service.ManagedDIDServiceImpl
import io.iohk.atala.agent.walletapi.sql.JdbcDIDNonSecretStorage
import io.iohk.atala.castor.controller.{DIDControllerImpl, DIDRegistrarControllerImpl}
import io.iohk.atala.castor.core.service.DIDServiceImpl
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.connect.controller.ConnectionControllerImpl
import io.iohk.atala.connect.core.service.ConnectionServiceImpl
import io.iohk.atala.connect.sql.repository.JdbcConnectionRepository
import io.iohk.atala.connect.sql.repository.Migrations as ConnectMigrations
import io.iohk.atala.issue.controller.IssueControllerImpl
import io.iohk.atala.mercury.*
import io.iohk.atala.pollux.core.service.CredentialServiceImpl
import io.iohk.atala.pollux.core.service.PresentationServiceImpl
import io.iohk.atala.pollux.core.service.VerificationPolicyServiceImpl
import io.iohk.atala.pollux.core.service.{CredentialSchemaServiceImpl, URIDereferencer, HttpURIDereferencerImpl}
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaController
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaControllerImpl
import io.iohk.atala.pollux.credentialschema.controller.VerificationPolicyControllerImpl
import io.iohk.atala.pollux.sql.repository.JdbcCredentialRepository
import io.iohk.atala.pollux.sql.repository.JdbcPresentationRepository
import io.iohk.atala.pollux.sql.repository.JdbcVerificationPolicyRepository
import io.iohk.atala.pollux.sql.repository.{JdbcCredentialSchemaRepository, Migrations as PolluxMigrations}
import io.iohk.atala.presentproof.controller.PresentProofControllerImpl
import io.iohk.atala.resolvers.DIDResolver
import io.iohk.atala.system.controller.SystemControllerImpl
import java.security.Security
import zio.*
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.{MetricsConfig, prometheus}
import zio.metrics.jvm.DefaultJvmMetrics

object MainApp extends ZIOAppDefault {

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

      app <- PrismAgentApp
        .run(didCommServicePort)
        .provide(
          didCommAgentLayer(didCommServiceUrl),
          DidCommX.liveLayer,
          // infra
          SystemModule.configLayer,
          ZioHttpClient.layer,
          // observability
          DefaultJvmMetrics.live.unit,
          SystemControllerImpl.layer,
          ZLayer.succeed(MetricsConfig(5.seconds)),
          prometheus.prometheusLayer,
          prometheus.publisherLayer,
          // controller
          ConnectionControllerImpl.layer,
          CredentialSchemaControllerImpl.layer,
          DIDControllerImpl.layer,
          DIDRegistrarControllerImpl.layer,
          IssueControllerImpl.layer,
          PresentProofControllerImpl.layer,
          VerificationPolicyControllerImpl.layer,
          // domain
          AppModule.apolloLayer,
          AppModule.didJwtResolverlayer,
          AppModule.seedResolverLayer,
          DIDOperationValidator.layer(),
          DIDResolver.layer,
          HttpURIDereferencerImpl.layer,
          // service
          ConnectionServiceImpl.layer,
          CredentialSchemaServiceImpl.layer,
          CredentialServiceImpl.layer,
          DIDServiceImpl.layer,
          ManagedDIDServiceImpl.layer,
          PresentationServiceImpl.layer,
          VerificationPolicyServiceImpl.layer,
          // grpc
          GrpcModule.irisStubLayer,
          GrpcModule.prismNodeStubLayer,
          // storage
          RepoModule.agentTransactorLayer >>> JdbcDIDNonSecretStorage.layer,
          RepoModule.connectTransactorLayer >>> JdbcConnectionRepository.layer,
          RepoModule.didSecretStorageLayer,
          RepoModule.polluxTransactorLayer >>> JdbcCredentialRepository.layer,
          RepoModule.polluxTransactorLayer >>> JdbcCredentialSchemaRepository.layer,
          RepoModule.polluxTransactorLayer >>> JdbcPresentationRepository.layer,
          RepoModule.polluxTransactorLayer >>> JdbcVerificationPolicyRepository.layer,
        )
    } yield app

    app.provide(
      RepoModule.polluxDbConfigLayer >>> PolluxMigrations.layer,
      RepoModule.connectDbConfigLayer >>> ConnectMigrations.layer,
      RepoModule.agentDbConfigLayer >>> AgentMigrations.layer,
    )
  }

}
