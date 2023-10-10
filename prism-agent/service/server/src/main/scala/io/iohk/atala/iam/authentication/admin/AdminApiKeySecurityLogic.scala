package io.iohk.atala.iam.authentication.admin

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.{AuthenticationError, Authenticator}
import sttp.tapir.EndpointIO
import sttp.tapir.ztapir.*
import zio.*

object AdminApiKeySecurityLogic {

  val adminApiKeyHeader: EndpointIO.Header[AdminApiKeyCredentials] = header[String]("x-admin-api-key")
    .mapTo[AdminApiKeyCredentials]
    .description("Admin API Key")

  // TODO: remove
  def securityLogic(credentials: AdminApiKeyCredentials)(
      authenticator: Authenticator[BaseEntity]
  ): IO[ErrorResponse, Entity] = ???

}
