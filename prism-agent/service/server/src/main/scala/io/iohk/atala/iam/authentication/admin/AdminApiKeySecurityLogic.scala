package io.iohk.atala.iam.authentication.admin

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.{AuthenticationError, Authenticator}
import sttp.tapir.EndpointIO
import sttp.tapir.ztapir.*
import zio.*

object AdminApiKeySecurityLogic {

  val adminApiKeyHeader: EndpointIO.Header[AdminApiKeyCredentials] = header[String]("x-admin-api-key")
    .mapTo[AdminApiKeyCredentials]
    .description("Admin API Key")

  def securityLogic[E <: BaseEntity](credentials: AdminApiKeyCredentials)(authenticator: Authenticator[E]): IO[ErrorResponse, E] =
    ZIO
      .succeed(authenticator)
      .flatMap(_.authenticate(credentials))
      .mapError(error => AuthenticationError.toErrorResponse(error))

}
