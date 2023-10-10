package io.iohk.atala.iam.authentication.apikey

import sttp.tapir.EndpointIO
import sttp.tapir.EndpointInput.Auth
import sttp.tapir.EndpointInput.AuthType.ApiKey
import sttp.tapir.ztapir.*

object ApiKeyEndpointSecurityLogic {
  val apiKeyHeader: Auth[ApiKeyCredentials, ApiKey] = auth.apiKey(
    header[Option[String]]("apikey")
      .mapTo[ApiKeyCredentials]
      .description("API key")
  )
}
