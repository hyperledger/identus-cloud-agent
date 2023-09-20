package io.iohk.atala.agent.server

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.http.ZioHttpClient
import io.iohk.atala.agent.server.sql.Migrations as AgentMigrations
import io.iohk.atala.agent.walletapi.service.{
  EntityServiceImpl,
  ManagedDIDService,
  ManagedDIDServiceWithEventNotificationImpl,
  WalletManagementServiceImpl
}
import io.iohk.atala.agent.walletapi.sql.{JdbcDIDNonSecretStorage, JdbcEntityRepository, JdbcWalletNonSecretStorage}
import io.iohk.atala.castor.controller.{DIDControllerImpl, DIDRegistrarControllerImpl}
import io.iohk.atala.castor.core.service.DIDServiceImpl
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.connect.controller.ConnectionControllerImpl
import io.iohk.atala.connect.core.service.{ConnectionServiceImpl, ConnectionServiceNotifier}
import io.iohk.atala.connect.sql.repository.{JdbcConnectionRepository, Migrations as ConnectMigrations}
import io.iohk.atala.event.controller.EventControllerImpl
import io.iohk.atala.event.notification.EventNotificationServiceImpl
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.admin.{AdminApiKeyAuthenticatorImpl, AdminConfig}
import io.iohk.atala.iam.authentication.apikey.{ApiKeyAuthenticatorImpl, ApiKeyConfig, JdbcAuthenticationRepository}
import io.iohk.atala.iam.entity.http.controller.{EntityController, EntityControllerImpl}
import io.iohk.atala.iam.wallet.http.controller.WalletManagementControllerImpl
import io.iohk.atala.issue.controller.IssueControllerImpl
import io.iohk.atala.mercury.*
import io.iohk.atala.pollux.core.service.*
import io.iohk.atala.pollux.credentialdefinition.controller.CredentialDefinitionControllerImpl
import io.iohk.atala.pollux.credentialschema.controller.{
  CredentialSchemaController,
  CredentialSchemaControllerImpl,
  VerificationPolicyControllerImpl
}
import io.iohk.atala.pollux.sql.repository.{
  JdbcCredentialDefinitionRepository,
  JdbcCredentialRepository,
  JdbcCredentialSchemaRepository,
  JdbcPresentationRepository,
  JdbcVerificationPolicyRepository,
  Migrations as PolluxMigrations
}
import io.iohk.atala.presentproof.controller.PresentProofControllerImpl
import io.iohk.atala.resolvers.DIDResolver
import io.iohk.atala.system.controller.SystemControllerImpl
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import zio.*
import zio.http.Client
import zio.metrics.connectors.micrometer
import zio.metrics.connectors.micrometer.MicrometerConfig
import zio.metrics.jvm.DefaultJvmMetrics

import java.security.Security
import scala.language.implicitConversions

object MainApp extends ZIOAppDefault {

  Security.insertProviderAt(BouncyCastleProviderSingleton.getInstance(), 2)

  // FIXME: remove this when db app user have correct privileges provisioned by k8s operator.
  // This should be executed before migration to have correct privilege for new objects.
  val preMigrations = for {
    _ <- ZIO.logInfo("running pre-migration steps.")
    appConfig <- ZIO.service[AppConfig].provide(SystemModule.configLayer)
    _ <- PolluxMigrations
      .initDbPrivileges(appConfig.pollux.database.appUsername)
      .provide(RepoModule.polluxTransactorLayer)
    _ <- ConnectMigrations
      .initDbPrivileges(appConfig.connect.database.appUsername)
      .provide(RepoModule.connectTransactorLayer)
    _ <- AgentMigrations
      .initDbPrivileges(appConfig.agent.database.appUsername)
      .provide(RepoModule.agentTransactorLayer)
  } yield ()

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

      _ <- preMigrations
      _ <- migrations

      app <- PrismAgentApp
        .run(didCommServicePort)
        .provide(
          DidCommX.liveLayer,
          // infra
          SystemModule.configLayer,
          AdminConfig.layer,
          ApiKeyConfig.layer,
          ZioHttpClient.layer,
          // observability
          DefaultJvmMetrics.live.unit,
          SystemControllerImpl.layer,
          ZLayer.succeed(PrometheusMeterRegistry(PrometheusConfig.DEFAULT)),
          ZLayer.succeed(MicrometerConfig.default),
          micrometer.micrometerLayer,
          // controller
          ConnectionControllerImpl.layer,
          CredentialSchemaControllerImpl.layer,
          CredentialDefinitionControllerImpl.layer,
          DIDControllerImpl.layer,
          DIDRegistrarControllerImpl.layer,
          IssueControllerImpl.layer,
          PresentProofControllerImpl.layer,
          VerificationPolicyControllerImpl.layer,
          EntityControllerImpl.layer,
          WalletManagementControllerImpl.layer,
          EventControllerImpl.layer,
          // domain
          AppModule.apolloLayer,
          AppModule.didJwtResolverlayer,
          DIDOperationValidator.layer(),
          DIDResolver.layer,
          HttpURIDereferencerImpl.layer,
          // service
          ConnectionServiceImpl.layer >>> ConnectionServiceNotifier.layer,
          CredentialSchemaServiceImpl.layer,
          CredentialDefinitionServiceImpl.layer,
          CredentialServiceImpl.layer >>> CredentialServiceNotifier.layer,
          DIDServiceImpl.layer,
          EntityServiceImpl.layer,
          ManagedDIDServiceWithEventNotificationImpl.layer,
          PresentationServiceImpl.layer >>> PresentationServiceNotifier.layer,
          VerificationPolicyServiceImpl.layer,
          WalletManagementServiceImpl.layer,
          // authentication
          AdminApiKeyAuthenticatorImpl.layer >+> ApiKeyAuthenticatorImpl.layer >+> DefaultAuthenticator.layer,
          // grpc
          GrpcModule.irisStubLayer,
          GrpcModule.prismNodeStubLayer,
          // storage
          RepoModule.agentContextAwareTransactorLayer ++ RepoModule.agentTransactorLayer >>> JdbcDIDNonSecretStorage.layer,
          RepoModule.agentContextAwareTransactorLayer >>> JdbcWalletNonSecretStorage.layer,
          RepoModule.allSecretStorageLayer,
          RepoModule.agentTransactorLayer >>> JdbcEntityRepository.layer,
          RepoModule.agentTransactorLayer >>> JdbcAuthenticationRepository.layer,
          RepoModule.connectContextAwareTransactorLayer >>> JdbcConnectionRepository.layer,
          RepoModule.polluxContextAwareTransactorLayer >>> JdbcCredentialRepository.layer,
          RepoModule.polluxContextAwareTransactorLayer ++ RepoModule.polluxTransactorLayer >>> JdbcCredentialSchemaRepository.layer,
          RepoModule.polluxContextAwareTransactorLayer ++ RepoModule.polluxTransactorLayer >>> JdbcCredentialDefinitionRepository.layer,
          RepoModule.polluxContextAwareTransactorLayer >>> JdbcPresentationRepository.layer,
          RepoModule.polluxContextAwareTransactorLayer >>> JdbcVerificationPolicyRepository.layer,
          // event notification service
          ZLayer.succeed(500) >>> EventNotificationServiceImpl.layer,
          // HTTP client
          Client.default,
          Scope.default
        )
    } yield app

    app.provide(
      RepoModule.polluxDbConfigLayer(appUser = false) >>> PolluxMigrations.layer,
      RepoModule.connectDbConfigLayer(appUser = false) >>> ConnectMigrations.layer,
      RepoModule.agentDbConfigLayer(appUser = false) >>> AgentMigrations.layer,
    )
  }

}
