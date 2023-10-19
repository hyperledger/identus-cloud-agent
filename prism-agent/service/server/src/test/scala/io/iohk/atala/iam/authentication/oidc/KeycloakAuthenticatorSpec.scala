package io.iohk.atala.iam.authentication.oidc

import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.service.WalletManagementServiceImpl
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.iam.authentication.AuthenticationError
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
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation
import zio.*
import zio.http.Client
import zio.test.*
import zio.test.Assertion.*

import java.net.URI
import scala.jdk.CollectionConverters.*

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

  private def createWalletResource(walletId: WalletId, name: String) =
    for {
      authzClient <- ZIO.service[AuthzClient]
      resource = {
        val resource = ResourceRepresentation()
        resource.setId(walletId.toUUID.toString())
        resource.setName(name)
        resource.setOwnerManagedAccess(true)
        resource
      }
      _ <- ZIO
        .attemptBlocking(authzClient.protection().resource().create(resource))
    } yield ()

  private def createUser(userId: String, password: String, enabled: Boolean = true) =
    for {
      adminClient <- adminClientZIO
      user = {
        val cred = CredentialRepresentation()
        cred.setTemporary(false)
        cred.setValue(password)

        val user = UserRepresentation()
        user.setId(userId)
        user.setUsername(userId)
        user.setEnabled(enabled)
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
    val s = suite("KeycloakAuthenticatorSepc")(authenticateSpec) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

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

  // TODO: add test about upgrading RPT
  private val authenticateSpec = suite("authenticate")(
    test("allow token with a permitted wallet") {
      for {
        client <- ZIO.service[KeycloakClient]
        authenticator <- ZIO.service[KeycloakAuthenticator]
        wallet <- ZIO.serviceWithZIO[WalletManagementService](_.createWallet(Wallet("wallet-1")))
        _ <- createWalletResource(wallet.id, "wallet-1")
        _ <- createUser("alice", "1234")
        _ <- createResourcePermission(wallet.id, "alice")
        token <- client.getAccessToken("alice", "1234").map(_.access_token)
        entity <- authenticator.authenticate(token)
        permittedWallet <- authenticator.authorize(entity)
      } yield assert(wallet.id)(equalTo(permittedWallet))
    },
    test("reject token with multiple permitted wallets") {
      for {
        client <- ZIO.service[KeycloakClient]
        authenticator <- ZIO.service[KeycloakAuthenticator]
        wallet1 <- ZIO.serviceWithZIO[WalletManagementService](_.createWallet(Wallet("wallet-1")))
        wallet2 <- ZIO.serviceWithZIO[WalletManagementService](_.createWallet(Wallet("wallet-2")))
        _ <- createWalletResource(wallet1.id, "wallet-1")
        _ <- createWalletResource(wallet2.id, "wallet-2")
        _ <- createUser("alice", "1234")
        _ <- createResourcePermission(wallet1.id, "alice")
        _ <- createResourcePermission(wallet2.id, "alice")
        token <- client.getAccessToken("alice", "1234").map(_.access_token)
        entity <- authenticator.authenticate(token)
        exit <- authenticator.authorize(entity).exit
      } yield assert(exit)(
        fails(
          isSubtype[AuthenticationError.UnexpectedError](
            hasField("message", _.message, containsString("Too many wallet"))
          )
        )
      )
    },
    test("reject malformed token") {
      for {
        client <- ZIO.service[KeycloakClient]
        authenticator <- ZIO.service[KeycloakAuthenticator]
        wallet <- ZIO.serviceWithZIO[WalletManagementService](_.createWallet(Wallet("wallet-1")))
        exit1 <- authenticator.authenticate("").exit
        exit2 <- authenticator.authenticate("what sorcery is this?").exit
      } yield assert(exit1)(fails(isSubtype[AuthenticationError.InvalidCredentials](anything)))
        && assert(exit2)(fails(isSubtype[AuthenticationError.InvalidCredentials](anything)))
    },
    test("reject malformed token") {
      for {
        client <- ZIO.service[KeycloakClient]
        authenticator <- ZIO.service[KeycloakAuthenticator]
        wallet <- ZIO.serviceWithZIO[WalletManagementService](_.createWallet(Wallet("wallet-1")))
        exit <- authenticator.authenticate("what sorcery is this?").exit
      } yield assert(exit)(fails(isSubtype[AuthenticationError.InvalidCredentials](anything)))
    },
    test("reject expired token") {
      for {
        client <- ZIO.service[KeycloakClient]
        authenticator <- ZIO.service[KeycloakAuthenticator]
        wallet <- ZIO.serviceWithZIO[WalletManagementService](_.createWallet(Wallet("wallet-1")))
        exit <- authenticator
          .authenticate(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICItTk9wRUNXdXhZMVg2b0ZGeUQyeWMzNzZZa1A5cWttY1JWU3prMjFmbm9rIn0.eyJleHAiOjE2OTc2OTAzNDQsImlhdCI6MTY5NzY5MDA0NCwianRpIjoiOTQ0Yjk0OTctNDA4NS00MjI0LTgxYWUtMjJhNjMwNzRhMmRhIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDozNjUxMy9yZWFsbXMvYXRhbGEtdGVzdCIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJkYjQzYzQ2Mi1iMWE1LTQxYzQtYjAxYi00ZWZlNjM5NzUwMjEiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJwcmlzbS1hZ2VudCIsInNlc3Npb25fc3RhdGUiOiIxMmFlOTE4Ny1jYzYzLTQ3ZTItOWNlNC05ZGZhNmE3MmEyN2UiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iLCJkZWZhdWx0LXJvbGVzLWF0YWxhLXRlc3QiXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJzaWQiOiIxMmFlOTE4Ny1jYzYzLTQ3ZTItOWNlNC05ZGZhNmE3MmEyN2UiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsInByZWZlcnJlZF91c2VybmFtZSI6ImFsaWNlIn0.GuFycLUFtMqyzk6OyYnB30b8jAAuIcLp60mzbEwppv82B54_ymh1EinU3T_urQLtLh31nWaPM6QU4yBK_mBF1Kbc35eXiEiDZKMwiJhEgXHQboHaiqKNCcoHrT2XjQTg4epT8Gv72a5PzsDw5BiILNDeAQlv-1YArOFSEB9uLjRYVFzE3dSDpd6kWCaISui6c9OaOsHuHJWbxVt9P8HxXA19Xu0HWSITxSHBTXDotwHBFZE32KYIF8aBZtgrZULsc8dyk6pHnDNh0fkPmoWhpgZ4hgakwAyhWURQQ0qloVscaL9joFmLzNsOEZAaN4ML9x1KtWygMYEcUa44Ses03w"
          )
          .exit
      } yield assert(exit)(fails(isSubtype[AuthenticationError.InvalidCredentials](anything)))
    },
    test("reject token with no permitted wallet") {
      for {
        client <- ZIO.service[KeycloakClient]
        authenticator <- ZIO.service[KeycloakAuthenticator]
        wallet <- ZIO.serviceWithZIO[WalletManagementService](_.createWallet(Wallet("wallet-1")))
        _ <- createUser("alice", "1234")
        token <- client.getAccessToken("alice", "1234").map(_.access_token)
        entity <- authenticator.authenticate(token)
        exit <- authenticator.authorize(entity).exit
      } yield assert(exit)(fails(isSubtype[AuthenticationError.ResourceNotPermitted](anything)))
    }
  )

}
