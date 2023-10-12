package io.iohk.atala.sharedtest.containers

import zio.test.TestAspect.*
import zio.test.*
import zio.{Scope, ZIO}

import scala.util.Try
object KeycloakTestContainerSupportSpec extends ZIOSpecDefault with KeycloakTestContainerSupport {

  override def spec = suite("KeycloakTestContainerSupportSpec")(
    test("Keycloak container should be started") {
      for {
        keycloakContainer <- ZIO.service[KeycloakContainerCustom]
      } yield assertTrue(keycloakContainer.container.isRunning)
    },
    test("Keycloak admin-client works") {
      for {
        adminClient <- ZIO.service[KeycloakAdminClient]
        usersCount <- ZIO.fromTry(Try(adminClient.realm("master").users().count()))
      } yield assertCompletes
    }
  ).provideLayerShared(keycloakContainerLayer >+> keycloakAdminClientLayer) @@ sequential
}
