package io.iohk.atala.iam.authorization.keycloak.admin

import io.iohk.atala.agent.walletapi.model.{Wallet, WalletSeed}
import io.iohk.atala.agent.walletapi.service.{WalletManagementService, WalletManagementServiceError}
import io.iohk.atala.event.notification.EventNotificationConfig
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
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import io.iohk.atala.sharedtest.containers.{KeycloakContainerCustom, KeycloakTestContainerSupport}
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
    with KeycloakConfigUtils {

  override def spec = suite("KeycloakPermissionManagementServiceSpec")(
    successfulCasesSuite,
    failureCasesSuite
  )

  val successfulCasesSuite = suite("Successful Cases")(
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
        entity = KeycloakEntity(id = UUID.fromString(user.getId), rawToken = "test")

        permissionService <- ZIO.service[PermissionManagement.Service[KeycloakEntity]]
        _ <- permissionService.grantWalletToUser(wallet.id, entity)

        token <- client.getAccessToken(username, password).map(_.access_token)

        entity <- authenticator.authenticate(token)
        permittedWallet <- authenticator.authorize(entity)
      } yield assert(wallet.id)(equalTo(permittedWallet))
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
        entity = KeycloakEntity(id = UUID.fromString(user.getId), rawToken = "test")

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
  ).provide(
    Client.default,
    keycloakContainerLayer,
    keycloakAdminConfigLayer,
    KeycloakAdmin.layer,
    KeycloakPermissionManagementService.layer,
    WalletManagementServiceStub.layer,
    KeycloakAuthenticatorImpl.layer,
    KeycloakClientImpl.authzClientLayer,
    ZLayer.fromZIO(initializeClient) >>> KeycloakClientImpl.layer,
    keycloakConfigLayer()
  ) @@ sequential

  val failureCasesSuite = suite("Failure Cases Suite")(
    test("grant wallet access to the user with invalid wallet id") {
      for {
        permissionService <- ZIO.service[PermissionManagement.Service[KeycloakEntity]]
        entity = KeycloakEntity(id = UUID.randomUUID(), rawToken = "")
        exit <- permissionService.grantWalletToUser(WalletId.random, entity).exit
      } yield assert(exit)(fails(isSubtype[WalletNotFoundById](anything)))
    }
  ).provide(
    keycloakContainerLayer,
    KeycloakPermissionManagementService.layer,
    WalletManagementServiceStub.layer,
    KeycloakClientImpl.authzClientLayer,
    keycloakConfigLayer()
  ) @@ sequential
}

class WalletManagementServiceStub extends WalletManagementService {
  private var wallets: Map[WalletId, Wallet] = Map.empty
  override def createWallet(wallet: Wallet, seed: Option[WalletSeed]): IO[WalletManagementServiceError, Wallet] = {
    val wallet = Wallet(name = "test")
    wallets = wallets + (wallet.id -> wallet)
    ZIO.succeed(wallet)
  }

  override def getWallet(walletId: WalletId): IO[WalletManagementServiceError, Option[Wallet]] = {
    ZIO.succeed(wallets.get(walletId))
  }

  override def getWallets(walletIds: Seq[WalletId]): IO[WalletManagementServiceError, Seq[Wallet]] = {
    ZIO.succeed(wallets.filter(w => walletIds.contains(w._1)).values.toSeq)
  }

  override def listWallets(
      offset: Option[RuntimeFlags],
      limit: Option[RuntimeFlags]
  ): IO[WalletManagementServiceError, (Seq[Wallet], RuntimeFlags)] = ???

  override def listWalletNotifications
      : ZIO[WalletAccessContext, WalletManagementServiceError, Seq[EventNotificationConfig]] = ???

  override def createWalletNotification(
      config: EventNotificationConfig
  ): ZIO[WalletAccessContext, WalletManagementServiceError, EventNotificationConfig] = ???

  override def deleteWalletNotification(
      id: _root_.java.util.UUID
  ): ZIO[WalletAccessContext, WalletManagementServiceError, Unit] = ???
}

object WalletManagementServiceStub {
  val layer = ZLayer.succeed(new WalletManagementServiceStub)
}
