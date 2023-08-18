package io.iohk.atala.iam.authentication.admin

import zio.IO
import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.admin.{
  AdminApiKeyAuthenticator,
  AdminApiKeyCredentials,
  EmptyAdminApiKeyCredentials
}
import zio.*
import sttp.model.HeaderNames
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.*
import zio.http.{HttpApp, Server}
import zio.{Console, ExitCode, IO, Scope, Task, ZIO, ZIOAppDefault, ZLayer}
import sttp.model.*
import zio.ZIO.logInfo
import io.iohk.atala.iam.authentication.{AuthenticationError, Authenticator, Credentials}
import sttp.tapir.json.zio.jsonBody

object AdminApiKeySecureEndpoint {

  val secureEndpoint
      : ZPartialServerEndpoint[Authenticator, AdminApiKeyCredentials, Entity, Unit, ErrorResponse, Unit, Any] =
    endpoint
      .securityIn(
        auth.apiKey(
          header[String]("x-admin-api-key").mapTo[AdminApiKeyCredentials]
        )
      )
      .errorOut(statusCode(StatusCode.Forbidden))
      .errorOut(jsonBody[ErrorResponse])
      .zServerSecurityLogic(secureEndpointLogic)

  private def secureEndpointLogic(credentials: AdminApiKeyCredentials): ZIO[Authenticator, ErrorResponse, Entity] =
    ZIO
      .service[Authenticator]
      .flatMap(_.authenticate(credentials))
      .mapError(error => AuthenticationError.toErrorResponse(error))

}
