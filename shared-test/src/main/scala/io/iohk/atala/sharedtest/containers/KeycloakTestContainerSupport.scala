package io.iohk.atala.sharedtest.containers

import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.{ClientRepresentation, RealmRepresentation}
import zio.*

type KeycloakAdminClient = Keycloak

trait KeycloakTestContainerSupport {
  protected val keycloakContainerLayer: TaskLayer[KeycloakContainerCustom] =
    KeycloakContainerCustom.layer

  protected val keycloakAdminClientLayer: URLayer[KeycloakContainerCustom, KeycloakAdminClient] =
    ZLayer.fromZIO(ZIO.service[KeycloakContainerCustom].map(_.container.getKeycloakAdminClient))

  protected val adminClientZIO = ZIO.service[KeycloakAdminClient]

  protected val realName = "atala-test"
  protected val realmRepresentation = {
    val rr = new RealmRepresentation()
    rr.setRealm(realName)
    rr
  }

  protected val agentClientSecret = "prism-agent-demo-secret"
  protected val agentClientRepresentation: ClientRepresentation = {
    val acr = new ClientRepresentation()
    acr.setClientId("prism-agent")
    acr.setDirectAccessGrantsEnabled(true)
    acr.setServiceAccountsEnabled(true)
    acr.setSecret(agentClientSecret)
    acr
  }
}
