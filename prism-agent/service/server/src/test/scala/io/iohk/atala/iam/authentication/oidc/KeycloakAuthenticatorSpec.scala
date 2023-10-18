package io.iohk.atala.iam.authentication.oidc

import io.iohk.atala.agent.walletapi.service.WalletManagementServiceImpl
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.sharedtest.containers.KeycloakAdminClient
import io.iohk.atala.sharedtest.containers.KeycloakContainerCustom
import io.iohk.atala.sharedtest.containers.KeycloakTestContainerSupport
import io.iohk.atala.sharedtest.containers.PostgresTestContainerSupport
import zio.*
import zio.http.Client
import zio.test.*

import java.net.URI
import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.test.container.DBTestUtils

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

  override def spec = {
    val suite1 = suite("KeycloakAuthenticatorSepc")(
      test("TODO - name this test") {
        for {
          authenticator <- ZIO.service[KeycloakAuthenticator]
        } yield assertCompletes
      }
    )
      @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)
      @@ TestAspect.tag("dev")

    suite1.provide(
      KeycloakAuthenticatorImpl.layer,
      ZLayer.fromZIO(initializeClient) >>> KeycloakClientImpl.layer,
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

}
