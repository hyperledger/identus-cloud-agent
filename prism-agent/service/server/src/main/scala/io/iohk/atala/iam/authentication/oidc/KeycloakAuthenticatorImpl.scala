package io.iohk.atala.iam.authentication.oidc

import zio.*
import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.agent.walletapi.model.Entity
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.representations.idm.authorization.AuthorizationRequest
import org.keycloak.authorization.client.{Configuration => KeycloakAuthzConfig}

import scala.jdk.CollectionConverters.*
import pdi.jwt.JwtCirce
import pdi.jwt.JwtOptions

class KeycloakAuthenticatorImpl(keycloakConfig: KeycloakConfig) extends KeycloakAuthenticator {

  // TODO: implement
  // TODO: from permitted resources, check the wallet
  override def authenticate(token: String): IO[AuthenticationError, Entity] = {
    ZIO
      .attemptBlocking {
        val config = KeycloakAuthzConfig(
          "http://localhost:9980",
          "atala-demo",
          "prism-agent",
          Map("secret" -> "prism-agent-demo-secret").asJava,
          null
        )
        val authzClient = AuthzClient.create(config)
        val request = AuthorizationRequest()
        val authResource = authzClient.authorization(token)
        val response = authResource.authorize(request)
        val rpt = response.getToken()
        rpt
      }
      .debug("RPT")
      .flatMap(decodeRpt)
      .orDie *> ZIO.fail(AuthenticationError.UnexpectedError("TODO: in-progress"))
  }

  override def isEnabled: Boolean = keycloakConfig.enabled

  private def decodeRpt(rpt: String): Task[Unit] = {
    ZIO
      .fromTry(JwtCirce.decodeRawAll(rpt, JwtOptions(false, false, false)))
      .debug("decoded")
      .unit
  }

}

object KeycloakAuthenticatorImpl {
  val layer: URLayer[KeycloakConfig, KeycloakAuthenticator] = ZLayer.fromFunction(KeycloakAuthenticatorImpl(_))
}
