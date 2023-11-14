package io.iohk.atala.iam.authorization.keycloak.admin

import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.service.WalletManagementServiceImpl
import io.iohk.atala.agent.walletapi.service.{WalletManagementService, WalletManagementServiceError}
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.iam.authentication.AuthenticationError.ResourceNotPermitted
import io.iohk.atala.iam.authentication.oidc.{
  KeycloakAuthenticator,
  KeycloakAuthenticatorImpl,
  KeycloakClient,
  KeycloakClientImpl,
  KeycloakEntity
}
import io.iohk.atala.iam.authorization.core.PermissionManagement
import io.iohk.atala.iam.authorization.core.PermissionManagement.Error.WalletNotFoundById
import io.iohk.atala.shared.models.WalletAdministrationContext
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import io.iohk.atala.sharedtest.containers.PostgresTestContainerSupport
import io.iohk.atala.sharedtest.containers.{KeycloakContainerCustom, KeycloakTestContainerSupport}
import io.iohk.atala.test.container.DBTestUtils
import zio.*
import zio.ZIO.*
import zio.http.Client
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

import java.util.UUID

object KeycloakPermissionManagementServiceSpec
    extends ZIOSpecDefault
    with KeycloakTestContainerSupport
    with KeycloakConfigUtils
    with PostgresTestContainerSupport
    with ApolloSpecHelper {

  override def spec = {
    val s = suite("KeycloakPermissionManagementServiceSpec")(
      successfulCasesSuite,
      failureCasesSuite,
      multitenantSuite
    ) @@ sequential @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

    s.provide(
      Client.default,
      keycloakContainerLayer,
      keycloakAdminConfigLayer,
      KeycloakAdmin.layer,
      KeycloakPermissionManagementService.layer,
      KeycloakAuthenticatorImpl.layer,
      ZLayer.fromZIO(initializeClient) >>> KeycloakClientImpl.authzClientLayer >+> KeycloakClientImpl.layer,
      keycloakConfigLayer(),
      WalletManagementServiceImpl.layer,
      JdbcWalletNonSecretStorage.layer,
      JdbcWalletSecretStorage.layer,
      contextAwareTransactorLayer,
      pgContainerLayer,
      apolloLayer
    ).provide(Runtime.removeDefaultLoggers)
  }

  private val successfulCasesSuite = suite("Successful Cases")(
    test("grant wallet access to the user") {
      for {
        client <- ZIO.service[KeycloakClient]
        authenticator <- ZIO.service[KeycloakAuthenticator]
        walletService <- ZIO.service[WalletManagementService]

        wallet <- walletService.createWallet(Wallet("test_1"))

        randomId = UUID.randomUUID().toString
        username = "user_" + randomId
        password = randomId
        user <- createUser(username = username, password = password)
        entity = KeycloakEntity(id = UUID.fromString(user.getId))

        permissionService <- ZIO.service[PermissionManagement.Service[KeycloakEntity]]
        _ <- permissionService.grantWalletToUser(wallet.id, entity)

        token <- client.getAccessToken(username, password).map(_.access_token)

        entity <- authenticator.authenticate(token)
        permittedWallet <- authenticator.authorize(entity)
      } yield assert(wallet.id)(equalTo(permittedWallet.walletId))
    },
    test("revoke the wallet access from the user") {
      for {
        client <- ZIO.service[KeycloakClient]
        authenticator <- ZIO.service[KeycloakAuthenticator]
        walletService <- ZIO.service[WalletManagementService]

        wallet <- walletService.createWallet(Wallet("test_2"))

        randomId = UUID.randomUUID().toString
        username = "user_" + randomId
        password = randomId
        user <- createUser(username = username, password = password)
        entity = KeycloakEntity(id = UUID.fromString(user.getId))

        permissionService <- ZIO.service[PermissionManagement.Service[KeycloakEntity]]
        _ <- permissionService.grantWalletToUser(wallet.id, entity)

        token <- client.getAccessToken(username, password).map(_.access_token)

        entity <- authenticator.authenticate(token)
        permittedWallet <- authenticator.authorize(entity)

        _ <- permissionService.revokeWalletFromUser(wallet.id, entity)

        token2 <- client.getAccessToken(username, password).map(_.access_token)
        entity2 <- authenticator.authenticate(token)
        permittedWallet2 <- authenticator.authorize(entity).exit

      } yield assert(permittedWallet2)(fails(isSubtype[ResourceNotPermitted](anything)))
    }
  ).provideSomeLayer(ZLayer.succeed(WalletAdministrationContext.Admin()))

  private val failureCasesSuite = suite("Failure Cases Suite")(
    test("grant wallet access to the user with invalid wallet id") {
      for {
        permissionService <- ZIO.service[PermissionManagement.Service[KeycloakEntity]]
        entity = KeycloakEntity(id = UUID.randomUUID())
        exit <- permissionService.grantWalletToUser(WalletId.random, entity).exit
      } yield assert(exit)(fails(isSubtype[WalletNotFoundById](anything)))
    }
  ).provideSomeLayer(ZLayer.succeed(WalletAdministrationContext.Admin()))

  private val multitenantSuite = suite("multi-tenant cases")(
    test("grant wallet access to the user by self-service") {
      val walletId = WalletId.random
      for {
        client <- ZIO.service[KeycloakClient]
        authenticator <- ZIO.service[KeycloakAuthenticator]
        walletService <- ZIO.service[WalletManagementService]

        wallet <- walletService
          .createWallet(Wallet("test_1", walletId))
          .provide(ZLayer.succeed(WalletAdministrationContext.Admin()))

        randomId = UUID.randomUUID().toString
        username = "user_" + randomId
        password = randomId
        user <- createUser(username = username, password = password)
        entity = KeycloakEntity(id = UUID.fromString(user.getId))

        permissionService <- ZIO.service[PermissionManagement.Service[KeycloakEntity]]
        _ <- permissionService
          .grantWalletToUser(wallet.id, entity)
          .provideSomeLayer(ZLayer.succeed(WalletAdministrationContext.SelfService(Seq(walletId))))

        token <- client.getAccessToken(username, password).map(_.access_token)

        entity <- authenticator.authenticate(token)
        permittedWallet <- authenticator.authorize(entity)
      } yield assert(wallet.id)(equalTo(permittedWallet.walletId))
    },
    test("grant wallet access to non-permitted wallet by self-service is not allowed") {
      val walletId = WalletId.random
      for {
        client <- ZIO.service[KeycloakClient]
        authenticator <- ZIO.service[KeycloakAuthenticator]
        walletService <- ZIO.service[WalletManagementService]

        wallet <- walletService
          .createWallet(Wallet("test_1", walletId))
          .provide(ZLayer.succeed(WalletAdministrationContext.Admin()))

        randomId = UUID.randomUUID().toString
        username = "user_" + randomId
        password = randomId
        user <- createUser(username = username, password = password)
        entity = KeycloakEntity(id = UUID.fromString(user.getId))

        permissionService <- ZIO.service[PermissionManagement.Service[KeycloakEntity]]
        exit <- permissionService
          .grantWalletToUser(WalletId.random, entity)
          .provideSomeLayer(ZLayer.succeed(WalletAdministrationContext.SelfService(Seq(walletId))))
          .exit
      } yield assert(exit)(fails(isSubtype[WalletNotFoundById](anything)))
    }
  )
}
