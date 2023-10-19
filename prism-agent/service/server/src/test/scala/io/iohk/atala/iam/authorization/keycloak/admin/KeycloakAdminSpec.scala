package io.iohk.atala.iam.authorization.keycloak.admin

import io.iohk.atala.sharedtest.containers.{KeycloakContainerCustom, KeycloakTestContainerSupport}
import zio.*
import zio.ZIO.*
import zio.test.Assertion.equalTo
import zio.test.TestAspect.*
import zio.test.*

import scala.util.Try

object KeycloakAdminSpec extends ZIOSpecDefault with KeycloakTestContainerSupport {

  private def keycloakAdminConfig: RIO[KeycloakContainerCustom, KeycloakAdminConfig] =
    for {
      keycloakContainer <- ZIO.service[KeycloakContainerCustom]
      keycloakAdminConfig = KeycloakAdminConfig(
        serverUrl = keycloakContainer.container.getAuthServerUrl,
        realm = "master",
        username = keycloakContainer.container.getAdminUsername,
        password = keycloakContainer.container.getAdminPassword,
        clientId = "admin-cli",
        clientSecret = Option.empty,
        authToken = Option.empty,
        scope = Option.empty
      )
    } yield keycloakAdminConfig

  val keycloakAdminConfigLayer = ZLayer.fromZIO(keycloakAdminConfig)

  override def spec = suite("KeycloakAdminSpec")(
    test("KeycloakAdmin can be created from the container") {
      for {
        keycloakContainer <- ZIO.service[KeycloakContainerCustom]
        config <- ZIO.service[KeycloakAdminConfig]

        keycloakAdmin <- KeycloakAdmin(config)
        usersCount <- ZIO.fromTry(
          Try(keycloakAdmin.realm("master").users().count()).map(_.toInt)
        )
      } yield assertTrue(keycloakContainer.container.isRunning) && assert(usersCount)(equalTo(1))
    }
  ).provideLayerShared(keycloakContainerLayer >+> keycloakAdminConfigLayer) @@ sequential

}
