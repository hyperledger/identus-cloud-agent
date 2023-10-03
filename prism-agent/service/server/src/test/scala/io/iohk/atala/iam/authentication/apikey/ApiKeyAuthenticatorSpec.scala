package io.iohk.atala.iam.authentication.apikey

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.model.{Entity, Wallet}
import io.iohk.atala.agent.walletapi.service.{
  EntityService,
  EntityServiceImpl,
  WalletManagementService,
  WalletManagementServiceImpl
}
import io.iohk.atala.agent.walletapi.sql.{JdbcEntityRepository, JdbcWalletNonSecretStorage, JdbcWalletSecretStorage}
import io.iohk.atala.container.util.MigrationAspects.*
import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.iam.authentication.AuthenticationError.InvalidCredentials
import io.iohk.atala.shared.models.WalletId
import io.iohk.atala.shared.test.containers.PostgresTestContainerSupport
import zio.Runtime.removeDefaultLoggers
import zio.test.Assertion.*
import zio.test.TestAspect.sequential
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assert, *}
import zio.{Scope, ULayer, ZIO, ZLayer}

object ApiKeyAuthenticatorSpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  private val apiKeyConfigDisabled = ApiKeyConfig(
    salt = "salt",
    enabled = false,
    authenticateAsDefaultUser = true,
    autoProvisioning = false
  )

  private val apiKeyConfigEnabledSingleTenant = apiKeyConfigDisabled.copy(enabled = true)
  private val apiKeyConfigEnabledMultiTenant = apiKeyConfigEnabledSingleTenant.copy(authenticateAsDefaultUser = false)
  private val apiKeyConfigEnabledMultiTenantWithAutoProvisioning =
    apiKeyConfigEnabledMultiTenant.copy(autoProvisioning = true)

  private def configLayer(config: ApiKeyConfig) = ZLayer.succeed(config)

  def apiKeyAuthenticatorLayer(apiKeyConfig: ApiKeyConfig) =
    ZLayer.makeSome[AuthenticationRepository & EntityService & WalletManagementService, ApiKeyAuthenticator](
      configLayer(apiKeyConfig) >>> ApiKeyAuthenticatorImpl.layer
    )

  val testEnvironmentLayer =
    ZLayer
      .make[WalletManagementService & EntityService & AuthenticationRepository & PostgreSQLContainer](
        JdbcAuthenticationRepository.layer,
        EntityServiceImpl.layer,
        JdbcEntityRepository.layer,
        Apollo.prism14Layer,
        WalletManagementServiceImpl.layer,
        JdbcWalletNonSecretStorage.layer,
        JdbcWalletSecretStorage.layer,
        systemTransactorLayer,
        contextAwareTransactorLayer,
        pgContainerLayer
      )

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    val testSuite = suite("ApiKeyAuthenticatorSpec")(
      authenticationDisabledSpec,
      authenticationEnabledSingleTenantSpec,
      authenticationEnabledMultiTenantSpec,
      authenticationEnabledMultiTenantSpecWithAutoProvisioning
    ) @@ sequential @@ migrate(
      schema = "public",
      paths = "classpath:sql/agent"
    )

    testSuite
      .provideSomeLayerShared(testEnvironmentLayer)
      .provide(removeDefaultLoggers)
  }

  val failWhenTheHeaderIsAnEmptyStringTest = test("should fail when the header is empty string")(
    for {
      exit <- ApiKeyAuthenticator.authenticate(ApiKeyCredentials(Option(""))).exit
    } yield assert(exit)(fails(isSubtype[ApiKeyAuthenticationError](anything)))
  )

  val failWhenTheHeaderIsNotProvidedTest = test("should fail when the header is not provided")(
    for {
      exit <- ApiKeyAuthenticator.authenticate(ApiKeyCredentials(Option.empty[String])).exit
    } yield assert(exit)(fails(isSubtype[InvalidCredentials](anything)))
  )

  val authenticationDisabledSpec = suite("when authentication disabled")(
    test("should fail with an error (logic is handled by the common auth module")(
      for {
        authenticator <- ZIO.service[ApiKeyAuthenticator]
        exit1 <- authenticator.authenticate(ApiKeyCredentials(Option.empty[String])).exit
        exit2 <- authenticator.authenticate(ApiKeyCredentials(Option("some key"))).exit
      } yield assert(exit1)(fails(isSubtype[AuthenticationError.AuthenticationMethodNotEnabled](anything))) &&
        assert(exit2)(fails(isSubtype[AuthenticationError.AuthenticationMethodNotEnabled](anything)))
    )
  ).provideSomeLayer(apiKeyAuthenticatorLayer(apiKeyConfigDisabled))

  val authenticationEnabledSingleTenantSpec = suite("when authentication enabled in the single tenant mode")(
    test("registered entity is authenticated as the default user")(
      for {
        authenticatedEntity <- ZIO.serviceWithZIO[ApiKeyAuthenticator](_.authenticate(ApiKeyCredentials(Option("key"))))
      } yield assert(authenticatedEntity)(equalTo(Entity.Default)),
    ),
    failWhenTheHeaderIsNotProvidedTest,
    failWhenTheHeaderIsAnEmptyStringTest
  ).provideSomeLayer(apiKeyAuthenticatorLayer(apiKeyConfigEnabledSingleTenant))

  val authenticationEnabledMultiTenantSpec = suite("when authentication enabled in the multi-tenant mode")(
    test("registered entity is authenticated")(
      for {
        wallet <- ZIO.serviceWithZIO[WalletManagementService](_.createWallet(Wallet("wallet", WalletId.random)))
        entity <- ZIO.serviceWithZIO[EntityService](_.create(Entity(name = "entity", walletId = wallet.id.toUUID)))
        _ <- ApiKeyAuthenticator.add(entity.id, "registeredkey#1")
        authenticatedEntity <- ZIO.serviceWithZIO[ApiKeyAuthenticator](
          _.authenticate(ApiKeyCredentials(Option("registeredkey#1")))
        )
      } yield assert(authenticatedEntity)(equalTo(entity))
    ),
    test("unregistered entity is not authenticated")(
      for {
        unauthenticatedEntity <- ApiKeyAuthenticator
          .authenticate(ApiKeyCredentials(Option("unregisteredkey")))
          .exit
      } yield assert(unauthenticatedEntity)(fails(isSubtype[AuthenticationError.InvalidCredentials](anything)))
    ),
    failWhenTheHeaderIsNotProvidedTest,
    failWhenTheHeaderIsAnEmptyStringTest
  ).provideSomeLayer(apiKeyAuthenticatorLayer(apiKeyConfigEnabledMultiTenant))

  val authenticationEnabledMultiTenantSpecWithAutoProvisioning =
    suite("when authentication enabled in the multi-tenant mode with auto-provisioning")(
      test("registered entity is authenticated")(
        for {
          wallet <- ZIO.serviceWithZIO[WalletManagementService](_.createWallet(Wallet("wallet", WalletId.random)))
          entity <- ZIO.serviceWithZIO[EntityService](_.create(Entity(name = "entity", walletId = wallet.id.toUUID)))
          _ <- ApiKeyAuthenticator.add(entity.id, "registered-key#1")
          authenticatedEntity <- ZIO.serviceWithZIO[ApiKeyAuthenticator](
            _.authenticate(ApiKeyCredentials(Option("registered-key#1")))
          )
        } yield assert(authenticatedEntity)(equalTo(entity))
      ),
      test("unregistered entity is authenticated (assuming that the apikey is valid)")(
        for {
          authenticatedEntity2 <- ApiKeyAuthenticator.authenticate(ApiKeyCredentials(Option("registered-key#2")))
          authenticatedEntity3 <- ApiKeyAuthenticator.authenticate(ApiKeyCredentials(Option("registered-key#3")))

          entity2 <- ZIO.serviceWithZIO[EntityService](_.getById(authenticatedEntity2.id))
          entity3 <- ZIO.serviceWithZIO[EntityService](_.getById(authenticatedEntity3.id))

          wallet2Exists <- ZIO
            .serviceWithZIO[WalletManagementService](_.listWallets())
            .map(tuple => tuple._1)
            .map(_.exists(_.id == entity2.walletId))
          wallet3Exists <- ZIO
            .serviceWithZIO[WalletManagementService](_.listWallets())
            .map(tuple => tuple._1)
            .map(_.exists(_.id == entity3.walletId))

        } yield assert(authenticatedEntity2)(equalTo(entity2)) && assert(authenticatedEntity3)(equalTo(entity3)) &&
          assert(wallet2Exists)(isTrue) && assert(wallet3Exists)(isTrue)
      ),
      failWhenTheHeaderIsNotProvidedTest,
      failWhenTheHeaderIsAnEmptyStringTest
    ).provideSomeLayer(apiKeyAuthenticatorLayer(apiKeyConfigEnabledMultiTenantWithAutoProvisioning))

}
