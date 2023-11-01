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

  protected val realmName = "atala-test"
  protected val realmRepresentation = {
    val rr = new RealmRepresentation()
    rr.setRealm(realmName)
    rr.setEnabled(true)
    rr
  }

  protected val agentClientSecret = "prism-agent-demo-secret"
  protected val agentClientRepresentation: ClientRepresentation = {
    val acr = new ClientRepresentation()
    acr.setClientId("prism-agent")
    acr.setAuthorizationServicesEnabled(true)
    acr.setDirectAccessGrantsEnabled(true)
    acr.setServiceAccountsEnabled(true)
    acr.setSecret(agentClientSecret)
    acr
  }

  protected def initializeClient =
    for {
      adminClient <- adminClientZIO
      _ <- ZIO.attemptBlocking(
        adminClient
          .realms()
          .create(realmRepresentation)
      )
      _ <- ZIO
        .attemptBlocking(
          adminClient
            .realm(realmRepresentation.getRealm)
            .clients()
            .create(agentClientRepresentation)
        )
    } yield ()
}
