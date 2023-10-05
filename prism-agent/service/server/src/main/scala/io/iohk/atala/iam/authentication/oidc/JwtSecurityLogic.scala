package io.iohk.atala.iam.authentication.oidc

import sttp.tapir.EndpointInput.Auth
import sttp.tapir.EndpointInput.AuthType.Http
import sttp.tapir.ztapir.*

object JwtSecurityLogic {
  val bearerAuthHeader: Auth[JwtCredentials, Http] = auth
    .bearer[Option[String]]()
    .mapTo[JwtCredentials]
}
