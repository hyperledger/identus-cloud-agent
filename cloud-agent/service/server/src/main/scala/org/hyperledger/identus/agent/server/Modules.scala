package org.hyperledger.identus.agent.server

import com.typesafe.config.ConfigFactory
import doobie.util.transactor.Transactor
import io.grpc.ManagedChannelBuilder
import org.hyperledger.identus.agent.server.config.AppConfig
import org.hyperledger.identus.agent.server.config.SecretStorageBackend
import org.hyperledger.identus.agent.server.config.ValidatedVaultConfig
import org.hyperledger.identus.agent.walletapi.service.EntityService
import org.hyperledger.identus.agent.walletapi.service.WalletManagementService
import org.hyperledger.identus.agent.walletapi.sql.{
  JdbcDIDSecretStorage,
  JdbcGenericSecretStorage,
  JdbcWalletSecretStorage
}
import org.hyperledger.identus.agent.walletapi.storage.{DIDSecretStorage, GenericSecretStorage, WalletSecretStorage}
import org.hyperledger.identus.agent.walletapi.vault.*
import org.hyperledger.identus.agent.walletapi.vault.{
  VaultDIDSecretStorage,
  VaultKVClient,
  VaultKVClientImpl,
  VaultWalletSecretStorage
}
import org.hyperledger.identus.castor.core.service.DIDService
import org.hyperledger.identus.iam.authentication.admin.AdminApiKeyAuthenticator
import org.hyperledger.identus.iam.authentication.admin.AdminApiKeyAuthenticatorImpl
import org.hyperledger.identus.iam.authentication.admin.AdminConfig
import org.hyperledger.identus.iam.authentication.apikey.ApiKeyAuthenticator
import org.hyperledger.identus.iam.authentication.apikey.ApiKeyAuthenticatorImpl
import org.hyperledger.identus.iam.authentication.apikey.ApiKeyConfig
import org.hyperledger.identus.iam.authentication.apikey.AuthenticationRepository
import org.hyperledger.identus.iam.authentication.oidc.KeycloakAuthenticator
import org.hyperledger.identus.iam.authentication.oidc.KeycloakAuthenticatorImpl
import org.hyperledger.identus.iam.authentication.oidc.KeycloakClientImpl
import org.hyperledger.identus.iam.authentication.oidc.KeycloakConfig
import org.hyperledger.identus.iam.authentication.oidc.KeycloakEntity
import org.hyperledger.identus.iam.authorization.core.PermissionManagement
import org.hyperledger.identus.iam.authorization.keycloak.admin.KeycloakPermissionManagementService
import org.hyperledger.identus.pollux.vc.jwt.{PrismDidResolver, DidResolver as JwtDidResolver}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import org.hyperledger.identus.shared.crypto.Apollo
import org.hyperledger.identus.shared.db.{ContextAwareTask, DbConfig, TransactorLayer}
import org.keycloak.authorization.client.AuthzClient
import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Client

object SystemModule {

  private val tmp: IO[Config.Error, AppConfig] =
    for {
      ret: AppConfig <- TypesafeConfigProvider
        .fromTypesafeConfig(ConfigFactory.load())
        .load(AppConfig.config)
      _ <- ZIO.log(s"HTTP server endpoint is setup as '${ret.agent.httpEndpoint.publicEndpointUrl}'")
      _ <- ZIO.log(s"DIDComm server endpoint is setup as '${ret.agent.didCommEndpoint.publicEndpointUrl}'")
    } yield ret

  val configLayer = ZLayer.fromZIO(tmp)

  val zioHttpClientLayer: ZLayer[Any, Throwable, Client] = {
    import zio.http.netty.NettyConfig
    import zio.http.{ConnectionPoolConfig, DnsResolver, ZClient}
    (ZLayer.fromZIO(
      for {
        appConfig <- ZIO.service[AppConfig].provide(SystemModule.configLayer)
      } yield ZClient.Config.default.copy(
        connectionPool = {
          val cpSize = appConfig.agent.httpClient.connectionPoolSize
          if (cpSize > 0) ConnectionPoolConfig.Fixed(cpSize)
          else ConnectionPoolConfig.Disabled
        },
        idleTimeout = Some(appConfig.agent.httpClient.idleTimeout),
        connectionTimeout = Some(appConfig.agent.httpClient.connectionTimeout),
      )
    ) ++
      ZLayer.succeed(NettyConfig.default) ++
      DnsResolver.default) >>> ZClient.live
  }
}

object AppModule {
  val apolloLayer: ULayer[Apollo] = Apollo.layer

  val didJwtResolverLayer: URLayer[DIDService, JwtDidResolver] =
    ZLayer.fromFunction(PrismDidResolver(_))

  val builtInAuthenticatorLayer: URLayer[
    AppConfig & AuthenticationRepository & EntityService & WalletManagementService,
    ApiKeyAuthenticator & AdminApiKeyAuthenticator
  ] =
    ZLayer.makeSome[
      AppConfig & AuthenticationRepository & EntityService & WalletManagementService,
      ApiKeyAuthenticator & AdminApiKeyAuthenticator
    ](
      AdminConfig.layer,
      ApiKeyConfig.layer,
      AdminApiKeyAuthenticatorImpl.layer,
      ApiKeyAuthenticatorImpl.layer,
    )

  val keycloakAuthenticatorLayer: RLayer[
    AppConfig & WalletManagementService & Client & PermissionManagement.Service[KeycloakEntity],
    KeycloakAuthenticator
  ] =
    ZLayer.fromZIO {
      ZIO
        .serviceWith[AppConfig](_.agent.authentication.keycloak.enabled)
        .map { isEnabled =>
          if (!isEnabled) KeycloakAuthenticatorImpl.disabled
          else
            ZLayer.makeSome[
              AppConfig & WalletManagementService & Client & PermissionManagement.Service[KeycloakEntity],
              KeycloakAuthenticator
            ](
              KeycloakConfig.layer,
              KeycloakAuthenticatorImpl.layer,
              KeycloakClientImpl.authzClientLayer,
              KeycloakClientImpl.layer
            )
        }
    }.flatten

  val keycloakPermissionManagementLayer
      : RLayer[AppConfig & WalletManagementService & Client, PermissionManagement.Service[KeycloakEntity]] = {
    ZLayer.fromZIO {
      ZIO
        .serviceWith[AppConfig](_.agent.authentication.keycloak.enabled)
        .map { isEnabled =>
          if (!isEnabled) KeycloakPermissionManagementService.disabled
          else
            ZLayer.makeSome[AppConfig & WalletManagementService & Client, PermissionManagement.Service[KeycloakEntity]](
              KeycloakClientImpl.authzClientLayer,
              KeycloakClientImpl.layer,
              KeycloakConfig.layer,
              KeycloakPermissionManagementService.layer
            )
        }
    }.flatten
  }
}

object GrpcModule {
  val prismNodeStubLayer: TaskLayer[NodeServiceGrpc.NodeServiceStub] = {
    val stubLayer = ZLayer.fromZIO(
      ZIO
        .service[AppConfig]
        .map(_.prismNode.service)
        .flatMap(config =>
          if (config.usePlainText) {
            ZIO.attempt(
              NodeServiceGrpc.stub(ManagedChannelBuilder.forAddress(config.host, config.port).usePlaintext.build)
            )
          } else {
            ZIO.attempt(
              NodeServiceGrpc.stub(ManagedChannelBuilder.forAddress(config.host, config.port).build)
            )
          }
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
    val vaultClient = ZLayer {
      for {
        config <- ZIO
          .service[AppConfig]
          .map(_.agent.secretStorage.vault)
          .someOrFailException
          .logError("Vault config is not found")
        _ <- ZIO.logInfo("Vault client config loaded. Address: " + config.address)
        vaultKVClient <- ZIO
          .fromEither(config.validate)
          .mapError(Exception(_))
          .flatMap {
            case ValidatedVaultConfig.TokenAuth(address, token) =>
              ZIO.logInfo("Using Vault token authentication") *> VaultKVClientImpl.fromToken(address, token)
            case ValidatedVaultConfig.AppRoleAuth(address, roleId, secretId) =>
              ZIO.logInfo("Using Vault AppRole authentication") *>
                VaultKVClientImpl.fromAppRole(address, roleId, secretId)
          }
      } yield vaultKVClient
    }

    SystemModule.configLayer ++ SystemModule.zioHttpClientLayer >>> vaultClient
  }

  val allSecretStorageLayer: TaskLayer[DIDSecretStorage & WalletSecretStorage & GenericSecretStorage] = {
    ZLayer.fromZIO {
      ZIO
        .service[AppConfig]
        .map(_.agent.secretStorage)
        .tap(conf => ZIO.logInfo(s"Using '${conf.backend}' as a secret storage backend"))
        .flatMap { conf =>
          val useSemanticPath = conf.vault.map(_.useSemanticPath).getOrElse(true)
          conf.backend match {
            case SecretStorageBackend.vault =>
              ZIO.succeed(
                ZLayer.make[DIDSecretStorage & WalletSecretStorage & GenericSecretStorage](
                  VaultDIDSecretStorage.layer(useSemanticPath),
                  VaultGenericSecretStorage.layer(useSemanticPath),
                  VaultWalletSecretStorage.layer,
                  vaultClientLayer,
                )
              )
            case SecretStorageBackend.postgres =>
              ZIO.succeed(
                ZLayer.make[DIDSecretStorage & WalletSecretStorage & GenericSecretStorage](
                  JdbcDIDSecretStorage.layer,
                  JdbcGenericSecretStorage.layer,
                  JdbcWalletSecretStorage.layer,
                  agentContextAwareTransactorLayer,
                )
              )
          }
        }
        .provide(SystemModule.configLayer)
    }.flatten
  }

}
