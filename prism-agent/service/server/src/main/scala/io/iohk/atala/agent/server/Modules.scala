package io.iohk.atala.agent.server

import com.typesafe.config.ConfigFactory
import doobie.util.transactor.Transactor
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.memory.DIDSecretStorageInMemory
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.sql.JdbcDIDSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.agent.walletapi.util.SeedResolver
import io.iohk.atala.agent.walletapi.vault.VaultDIDSecretStorage
import io.iohk.atala.agent.walletapi.vault.VaultKVClient
import io.iohk.atala.agent.walletapi.vault.VaultKVClientImpl
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.pollux.sql.repository.DbConfig as PolluxDbConfig
import io.iohk.atala.pollux.vc.jwt.DidResolver as JwtDidResolver
import io.iohk.atala.pollux.vc.jwt.PrismDidResolver
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.db.DbConfig
import io.iohk.atala.shared.db.TransactorLayer
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import zio.config.ReadError
import zio.config.read
import zio.config.typesafe.TypesafeConfigSource
import zio.config.{ReadError, read}
import zio.interop.catz.*

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

  val seedResolverLayer =
    ZLayer.make[SeedResolver](
      ZLayer.fromFunction((config: AppConfig) => SeedResolver.layer(isDevMode = config.devMode)).flatten,
      apolloLayer,
      SystemModule.configLayer
    )

  val didJwtResolverlayer: URLayer[DIDService, JwtDidResolver] =
    ZLayer.fromFunction(PrismDidResolver(_))

  // FIXME
  // Create default wallet and provide it as a global layer.
  // This should be removed when we support dynamic wallet creation.
  val defaultWalletContext: RLayer[SeedResolver & WalletManagementService, WalletAccessContext] = ZLayer.fromZIO {
    val useNewWallet = false // to create a new wallet or use existing one globally
    for {
      svc <- ZIO.service[WalletManagementService]
      seed <- ZIO.serviceWithZIO[SeedResolver](_.resolve)
      walletId <-
        if (useNewWallet) svc.createWallet(Some(seed))
        else svc.listWallets().map(_._1).map(_.headOption).someOrElseZIO(svc.createWallet(Some(seed)))
      _ <- ZIO.debug(s"Global wallet id: $walletId")
    } yield WalletAccessContext(walletId)
  }
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

  val polluxTransactorLayer: TaskLayer[Transactor[ContextAwareTask]] =
    polluxDbConfigLayer() >>> TransactorLayer.contextAwareTask

  val connectDbConfigLayer: TaskLayer[DbConfig] = {
    val dbConfigLayer = ZLayer.fromZIO {
      ZIO.service[AppConfig].map(_.connect.database).map(_.dbConfig(appUser = false))
    }
    SystemModule.configLayer >>> dbConfigLayer
  }

  val connectTransactorLayer: TaskLayer[Transactor[ContextAwareTask]] =
    connectDbConfigLayer >>> TransactorLayer.contextAwareTask

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

  val allSecretStorageLayer: TaskLayer[DIDSecretStorage & WalletSecretStorage] = {
    ZLayer.fromZIO {
      ZIO
        .service[AppConfig]
        .map(_.agent.secretStorage.backend)
        .tap(backend => ZIO.logInfo(s"Using '$backend' as a secret storage backend"))
        .flatMap {
          case "vault" =>
            ZIO.succeed(
              ZLayer.make[DIDSecretStorage & WalletSecretStorage](
                VaultDIDSecretStorage.layer,
                VaultWalletSecretStorage.layer,
                vaultClientLayer,
              )
            )
          case "postgres" =>
            ZIO.succeed(
              ZLayer.make[DIDSecretStorage & WalletSecretStorage](
                JdbcDIDSecretStorage.layer,
                JdbcWalletSecretStorage.layer,
                agentContextAwareTransactorLayer,
              )
            )
          case "memory" =>
            ZIO.succeed(
              ZLayer.make[DIDSecretStorage](
                DIDSecretStorageInMemory.layer
              )
            )
          case backend =>
            ZIO
              .fail(s"Unsupported secret storage backend $backend. Available options are 'postgres', 'vault'")
              .tapError(msg => ZIO.logError(msg))
              .mapError(msg => Exception(msg))
        }
        .provide(SystemModule.configLayer)
    }.flatten
  }

}
