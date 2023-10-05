package io.iohk.atala.iam.authentication.apikey

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.{AuthenticationError, Authenticator}
import sttp.tapir.EndpointIO
import sttp.tapir.EndpointInput.Auth
import sttp.tapir.EndpointInput.AuthType.ApiKey
import sttp.tapir.ztapir.*
import zio.*

object ApiKeyEndpointSecurityLogic {

  val apiKeyHeader: Auth[ApiKeyCredentials, ApiKey] = auth.apiKey(
    header[Option[String]]("apikey")
      .mapTo[ApiKeyCredentials]
      .description("API key")
  )

  // TODO: remove
  def securityLogic(credentials: ApiKeyCredentials)(authenticator: Authenticator): IO[ErrorResponse, Entity] =
    authenticator.authenticate(credentials).mapError(error => AuthenticationError.toErrorResponse(error))

}
