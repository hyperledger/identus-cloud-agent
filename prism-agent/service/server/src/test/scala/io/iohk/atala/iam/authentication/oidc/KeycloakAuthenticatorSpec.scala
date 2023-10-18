package io.iohk.atala.iam.authentication.oidc

import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.service.WalletManagementServiceImpl
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.shared.models.WalletId
import io.iohk.atala.sharedtest.containers.KeycloakAdminClient
import io.iohk.atala.sharedtest.containers.KeycloakContainerCustom
import io.iohk.atala.sharedtest.containers.KeycloakTestContainerSupport
import io.iohk.atala.sharedtest.containers.PostgresTestContainerSupport
import io.iohk.atala.test.container.DBTestUtils
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.keycloak.representations.idm.authorization.ResourceRepresentation
import zio.*
import zio.http.Client
import zio.test.*
import zio.test.Assertion.*

import java.net.URI
import scala.jdk.CollectionConverters.*
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation

object KeycloakAuthenticatorSpec
    extends ZIOSpecDefault,
      KeycloakTestContainerSupport,
      PostgresTestContainerSupport,
      ApolloSpecHelper {

  private val keycloakConfigLayer =
    ZLayer.fromZIO {
      ZIO.serviceWith[KeycloakContainerCustom] { container =>
        val host = container.container.getHost()
        val port = container.container.getHttpPort()
        val url = s"http://${host}:${port}"
        KeycloakConfig(
          enabled = true,
          keycloakUrl = URI(url).toURL(),
          realmName = realmName,
          clientId = agentClientRepresentation.getClientId(),
          clientSecret = agentClientSecret,
          autoUpgradeToRPT = true
        )
      }
    }

  private def createWalletResource(walletId: WalletId) =
    for {
      authzClient <- ZIO.service[AuthzClient]
      resource = {
        val resource = ResourceRepresentation()
        resource.setId(walletId.toUUID.toString())
        resource.setName("wallet-1")
        resource.setOwnerManagedAccess(true)
        resource
      }
      _ <- ZIO
        .attemptBlocking(authzClient.protection().resource().create(resource))
    } yield ()

  private def createUser(userId: String, password: String) =
    for {
      adminClient <- adminClientZIO
      user = {
        val cred = CredentialRepresentation()
        cred.setTemporary(false)
        cred.setValue(password)

        val user = UserRepresentation()
        user.setId(userId)
        user.setUsername(userId)
        user.setEnabled(true)
        user.setCredentials(List(cred).asJava)
        user
      }
      _ <- ZIO.attemptBlocking(adminClient.realm(realmName).users().create(user))
    } yield ()

  private def createResourcePermission(walletId: WalletId, userId: String) =
    for {
      authzClient <- ZIO.service[AuthzClient]
      resourceId = walletId.toUUID.toString()
      policy = {
        val policy = UmaPermissionRepresentation()
        policy.setName(s"${userId} on wallet ${resourceId} permission")
        policy.setUsers(Set(userId).asJava)
        policy
      }
      _ <- ZIO.attemptBlocking(authzClient.protection().policy(resourceId).create(policy))
    } yield ()

  override def spec = {
    val s = suite("KeycloakAuthenticatorSepc")(authenticateSpec)
      @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)
      @@ TestAspect.tag("dev")

    s.provide(
      KeycloakAuthenticatorImpl.layer,
      ZLayer.fromZIO(initializeClient) >>> KeycloakClientImpl.layer ++ KeycloakClientImpl.authzClientLayer,
      keycloakConfigLayer,
      keycloakAdminClientLayer,
      keycloakContainerLayer,
      Client.default,
      WalletManagementServiceImpl.layer,
      JdbcWalletNonSecretStorage.layer,
      JdbcWalletSecretStorage.layer,
      contextAwareTransactorLayer,
      pgContainerLayer,
      apolloLayer
    )
  }

  private val authenticateSpec = suite("authenticate")(
    test("return permitted wallet") {
      for {
        client <- ZIO.service[KeycloakClient]
        authenticator <- ZIO.service[KeycloakAuthenticator]
        wallet <- ZIO.serviceWithZIO[WalletManagementService](_.createWallet(Wallet("wallet-1")))
        _ <- createWalletResource(wallet.id)
        _ <- createUser("alice", "1234")
        _ <- createResourcePermission(wallet.id, "alice")
        token <- client.getAccessToken("alice", "1234").map(_.access_token)
        entity <- authenticator.authenticate(token)
        permittedWallet <- authenticator.authorize(entity)
      } yield assert(wallet.id)(equalTo(permittedWallet))
    }
  )

}
