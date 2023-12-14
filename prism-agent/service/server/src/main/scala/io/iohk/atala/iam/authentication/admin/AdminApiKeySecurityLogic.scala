package io.iohk.atala.iam.authentication.admin

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.{AuthenticationError, Authenticator}
import sttp.tapir.EndpointIO
import sttp.tapir.EndpointInput.Auth
import sttp.tapir.EndpointInput.AuthType.ApiKey
import sttp.tapir.ztapir.*
import zio.*

object AdminApiKeySecurityLogic {

  val adminApiKeyHeader: Auth[AdminApiKeyCredentials, ApiKey] = auth
    .apiKey(
      header[Option[String]]("x-admin-api-key")
        .mapTo[AdminApiKeyCredentials]
        .description("Admin API Key")
    )
    .securitySchemeName("adminApiKeyAuth")

  def securityLogic[E <: BaseEntity](
      credentials: AdminApiKeyCredentials
  )(authenticator: Authenticator[E]): IO[ErrorResponse, E] =
    ZIO
      .succeed(authenticator)
      .flatMap(_.authenticate(credentials))
      .mapError(error => AuthenticationError.toErrorResponse(error))

}
