package io.iohk.atala.agent.server

import com.typesafe.config.ConfigFactory
import doobie.util.transactor.Transactor
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.config.SecretStorageBackend
import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.memory.{
  DIDSecretStorageInMemory,
  GenericSecretStorageInMemory,
  WalletSecretStorageInMemory
}
import io.iohk.atala.agent.walletapi.service.EntityService
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.sql.{JdbcDIDSecretStorage, JdbcGenericSecretStorage, JdbcWalletSecretStorage}
import io.iohk.atala.agent.walletapi.storage.{DIDSecretStorage, GenericSecretStorage, WalletSecretStorage}
import io.iohk.atala.agent.walletapi.vault.*
import io.iohk.atala.agent.walletapi.vault.{
  VaultDIDSecretStorage,
  VaultKVClient,
  VaultKVClientImpl,
  VaultWalletSecretStorage
}
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.admin.AdminApiKeyAuthenticator
import io.iohk.atala.iam.authentication.admin.AdminApiKeyAuthenticatorImpl
import io.iohk.atala.iam.authentication.admin.AdminConfig
import io.iohk.atala.iam.authentication.apikey.ApiKeyAuthenticator
import io.iohk.atala.iam.authentication.apikey.ApiKeyAuthenticatorImpl
import io.iohk.atala.iam.authentication.apikey.ApiKeyConfig
import io.iohk.atala.iam.authentication.apikey.AuthenticationRepository
import io.iohk.atala.iam.authentication.oidc.KeycloakAuthenticatorImpl
import io.iohk.atala.iam.authentication.oidc.KeycloakClientImpl
import io.iohk.atala.iam.authentication.oidc.KeycloakConfig
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.pollux.vc.jwt.{PrismDidResolver, DidResolver as JwtDidResolver}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.shared.db.{ContextAwareTask, DbConfig, TransactorLayer}
import zio.*
import zio.config.typesafe.TypesafeConfigSource
import zio.config.{ReadError, read}
import zio.http.Client

object SystemModule {
  val configLayer: Layer[ReadError[String], AppConfig] = ZLayer.fromZIO {
    read(
      AppConfig.descriptor.from(
        TypesafeConfigSource.fromTypesafeConfig(
          ZIO.attempt(ConfigFactory.load())
        )
      )
    )
  }
}

object AppModule {
  val apolloLayer: ULayer[Apollo] = Apollo.prism14Layer

  val didJwtResolverLayer: URLayer[DIDService, JwtDidResolver] =
    ZLayer.fromFunction(PrismDidResolver(_))

  val authenticatorLayer: RLayer[
    AppConfig & WalletManagementService & AuthenticationRepository & EntityService & Client,
    DefaultAuthenticator & ApiKeyAuthenticator
  ] =
    ZLayer.makeSome[
      AppConfig & WalletManagementService & AuthenticationRepository & EntityService & Client,
      DefaultAuthenticator & ApiKeyAuthenticator
    ](
      AdminConfig.layer,
      ApiKeyConfig.layer,
      KeycloakConfig.layer,
      DefaultAuthenticator.layer,
      AdminApiKeyAuthenticatorImpl.layer,
      ApiKeyAuthenticatorImpl.layer,
      KeycloakAuthenticatorImpl.layer,
      KeycloakClientImpl.layer
    )
}

object GrpcModule {
  // TODO: once Castor + Pollux has migrated to use Node 2.0 stubs, this should be removed.
  val irisStubLayer: TaskLayer[IrisServiceStub] = {
    val stubLayer = ZLayer.fromZIO(
      ZIO
        .service[AppConfig]
        .map(_.iris.service)
        .flatMap(config =>
          ZIO.attempt(
            IrisServiceGrpc.stub(ManagedChannelBuilder.forAddress(config.host, config.port).usePlaintext.build)
          )
        )
    )
    SystemModule.configLayer >>> stubLayer
  }

  val prismNodeStubLayer: TaskLayer[NodeServiceGrpc.NodeServiceStub] = {
    val stubLayer = ZLayer.fromZIO(
      ZIO
        .service[AppConfig]
        .map(_.prismNode.service)
        .flatMap(config =>
          ZIO.attempt(
            NodeServiceGrpc.stub(ManagedChannelBuilder.forAddress(config.host, config.port).usePlaintext.build)
          )
        )
    )
    SystemModule.configLayer >>> stubLayer
  }
}

object RepoModule {

  def polluxDbConfigLayer(appUser: Boolean = true): TaskLayer[DbConfig] = {
    val dbConfigLayer = ZLayer.fromZIO {
      ZIO.service[AppConfig].map(_.pollux.database).map(_.dbConfig(appUser = appUser))
    }
    SystemModule.configLayer >>> dbConfigLayer
  }

  val polluxContextAwareTransactorLayer: TaskLayer[Transactor[ContextAwareTask]] =
    polluxDbConfigLayer() >>> TransactorLayer.contextAwareTask

  val polluxTransactorLayer: TaskLayer[Transactor[Task]] =
    polluxDbConfigLayer(appUser = false) >>> TransactorLayer.task

  def connectDbConfigLayer(appUser: Boolean = true): TaskLayer[DbConfig] = {
    val dbConfigLayer = ZLayer.fromZIO {
      ZIO.service[AppConfig].map(_.connect.database).map(_.dbConfig(appUser = appUser))
    }
    SystemModule.configLayer >>> dbConfigLayer
  }

  val connectContextAwareTransactorLayer: TaskLayer[Transactor[ContextAwareTask]] =
    connectDbConfigLayer() >>> TransactorLayer.contextAwareTask

  val connectTransactorLayer: TaskLayer[Transactor[Task]] =
    connectDbConfigLayer(appUser = false) >>> TransactorLayer.task

  def agentDbConfigLayer(appUser: Boolean = true): TaskLayer[DbConfig] = {
    val dbConfigLayer = ZLayer.fromZIO {
      ZIO.service[AppConfig].map(_.agent.database).map(_.dbConfig(appUser = appUser))
    }
    SystemModule.configLayer >>> dbConfigLayer
  }

  val agentContextAwareTransactorLayer: TaskLayer[Transactor[ContextAwareTask]] =
    agentDbConfigLayer() >>> TransactorLayer.contextAwareTask

  val agentTransactorLayer: TaskLayer[Transactor[Task]] =
    agentDbConfigLayer(appUser = false) >>> TransactorLayer.task

  val vaultClientLayer: TaskLayer[VaultKVClient] = {
    val vaultClientConfig = ZLayer {
      for {
        config <- ZIO
          .service[AppConfig]
          .map(_.agent.secretStorage.vault)
          .someOrFailException
          .tapError(_ => ZIO.logError("Vault config is not found"))
        _ <- ZIO.logInfo("Vault client config loaded. Address: " + config.address)
        vaultKVClient <- VaultKVClientImpl.fromAddressAndToken(config.address, config.token)
      } yield vaultKVClient
    }

    SystemModule.configLayer >>> vaultClientConfig
  }

  val allSecretStorageLayer: TaskLayer[DIDSecretStorage & WalletSecretStorage & GenericSecretStorage] = {
    ZLayer.fromZIO {
      ZIO
        .service[AppConfig]
        .map(_.agent.secretStorage.backend)
        .tap(backend => ZIO.logInfo(s"Using '$backend' as a secret storage backend"))
        .flatMap {
          case SecretStorageBackend.vault =>
            ZIO.succeed(
              ZLayer.make[DIDSecretStorage & WalletSecretStorage & GenericSecretStorage](
                VaultDIDSecretStorage.layer,
                VaultWalletSecretStorage.layer,
                VaultGenericSecretStorage.layer,
                vaultClientLayer,
              )
            )
          case SecretStorageBackend.postgres =>
            ZIO.succeed(
              ZLayer.make[DIDSecretStorage & WalletSecretStorage & GenericSecretStorage](
                JdbcDIDSecretStorage.layer,
                JdbcWalletSecretStorage.layer,
                JdbcGenericSecretStorage.layer,
                agentContextAwareTransactorLayer,
              )
            )
          case SecretStorageBackend.memory =>
            ZIO.succeed(
              ZLayer.make[DIDSecretStorage & WalletSecretStorage & GenericSecretStorage](
                DIDSecretStorageInMemory.layer,
                WalletSecretStorageInMemory.layer,
                GenericSecretStorageInMemory.layer
              )
            )
        }
        .provide(SystemModule.configLayer)
    }.flatten
  }

}
