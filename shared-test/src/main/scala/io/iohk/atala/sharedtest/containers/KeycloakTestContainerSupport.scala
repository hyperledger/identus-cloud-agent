package io.iohk.atala.sharedtest.containers
import org.keycloak.admin.client.Keycloak
import zio.*

type KeycloakAdminClient = Keycloak

trait KeycloakTestContainerSupport {
  protected val keycloakContainerLayer: TaskLayer[KeycloakContainerCustom] =
    KeycloakContainerCustom.layer

  protected val keycloakAdminClientLayer: URLayer[KeycloakContainerCustom, KeycloakAdminClient] =
    ZLayer.fromZIO(ZIO.service[KeycloakContainerCustom].map(_.container.getKeycloakAdminClient))
}
