package io.iohk.atala.agent.server

import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.service.EntityServiceImpl
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.service.WalletManagementServiceImpl
import io.iohk.atala.agent.walletapi.sql.JdbcEntityRepository
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.iam.authentication.apikey.ApiKeyAuthenticatorImpl
import io.iohk.atala.iam.authentication.apikey.JdbcAuthenticationRepository
import io.iohk.atala.shared.models.WalletId
import io.iohk.atala.shared.test.containers.PostgresTestContainerSupport
import io.iohk.atala.test.container.DBTestUtils
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.ZIOSpecDefault

object AgentInitializationSpec extends ZIOSpecDefault, PostgresTestContainerSupport, ApolloSpecHelper {

  override def spec = {
    val s = suite("AgentInitialization")(
      initializeDefaultWalletSpec
    ) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

    s.provide(
      SystemModule.configLayer,
      ZLayer.fromFunction((appConfig: AppConfig) => appConfig.agent.authentication.apiKey),
      WalletManagementServiceImpl.layer,
      ApiKeyAuthenticatorImpl.layer,
      EntityServiceImpl.layer,
      JdbcWalletNonSecretStorage.layer,
      JdbcWalletSecretStorage.layer,
      JdbcAuthenticationRepository.layer,
      JdbcEntityRepository.layer,
      contextAwareTransactorLayer,
      systemTransactorLayer,
      apolloLayer,
      pgContainerLayer
    )
  }

  private val initializeDefaultWalletSpec = suite("initializeDefaultWallet")(
    test("do not create default wallet if disabled") {
      for {
        _ <- AgentInitialization.run.overrideConfig(enableDefaultWallet = false)
        wallets <- ZIO.serviceWithZIO[WalletManagementService](_.listWallets()).map(_._1)
      } yield assert(wallets)(isEmpty)
    },
    test("create default wallet if enabled") {
      for {
        _ <- AgentInitialization.run.overrideConfig(enableDefaultWallet = true)
        wallets <- ZIO.serviceWithZIO[WalletManagementService](_.listWallets()).map(_._1)
      } yield assert(wallets)(hasSize(equalTo(1))) &&
        assert(wallets.headOption.map(_.id))(isSome(equalTo(WalletId.default)))
    }
  ) @@ TestAspect.tag("dev")

  extension [R <: AppConfig, E, A](effect: ZIO[R, E, A]) {
    def overrideConfig(enableDefaultWallet: Boolean = true): ZIO[R, E, A] = {
      for {
        appConfig <- ZIO.service[AppConfig]
        agentConfig = appConfig.agent
        defaultWalletConfig = agentConfig.defaultWallet
        result <- effect.provideSomeLayer(
          ZLayer.succeed(
            appConfig.copy(
              agent = agentConfig.copy(
                defaultWallet = defaultWalletConfig.copy(enabled = enableDefaultWallet)
              )
            )
          )
        )
      } yield result
    }
  }

}
