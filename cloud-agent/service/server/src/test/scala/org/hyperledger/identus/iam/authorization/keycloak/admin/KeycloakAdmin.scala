package org.hyperledger.identus.iam.authorization.keycloak.admin

import org.hyperledger.identus.sharedtest.containers.KeycloakAdminClient
import org.keycloak.admin.client.Keycloak
import zio.{RLayer, Task, ZIO, ZLayer}

import scala.util.Try
// TODO: move to shared module closed to the KeycloakTestContainerSupport class

// The following arguments are available:
// serverUrl: String,
// realm: String,
// username: String,
// password: String,
// clientId: String,
// clientSecret: String,
// sslContext: SSLContext,
// customJacksonProvider: AnyRef, - should be skipped
// disableTrustManager: Boolean, - false by default
// authToken: String, - can be skipped
// scope: String - can be skipped
// TODO: Ssl context is not supported yet
case class KeycloakAdminConfig(
    serverUrl: String,
    realm: String,
    username: String,
    password: String,
    clientId: String,
    clientSecret: Option[String],
    authToken: Option[String],
    scope: Option[String]
) {
  def isHttps: Boolean = serverUrl.startsWith("https")
}

object KeycloakAdmin {

  def apply(config: KeycloakAdminConfig): Task[KeycloakAdminClient] = {
    if (config.isHttps)
      ZIO.fail(new Exception("Ssl is not supported yet"))
    else
      ZIO.fromTry[KeycloakAdminClient](
        Try[KeycloakAdminClient](
          Keycloak.getInstance(
            config.serverUrl,
            config.realm,
            config.username,
            config.password,
            config.clientId,
            config.clientSecret.orNull
          )
        )
      )
  }

  val layer: RLayer[KeycloakAdminConfig, KeycloakAdminClient] =
    ZLayer.fromZIO(ZIO.service[KeycloakAdminConfig].flatMap(config => apply(config)))
}
