package io.iohk.atala.verification.controller

import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.iam.authentication.oidc.JwtSecurityLogic.jwtAuthHeader
import io.iohk.atala.presentproof.controller.http.*
import io.iohk.atala.system.controller.http.HealthInfo
import sttp.apispec.Tag
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.stringBody

import java.util.UUID

object VcVerificationEndpoints {
  val verify: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, http.VcVerificationRequests),
    ErrorResponse,
    http.VcVerificationResponses,
    Any
  ] =
    endpoint.post
      .tag("Verifiable Credentials Verification")
      .name("verify")
      .summary("As a Verifier, verify a set of credentials")
      .description("As a Verifier, verify a set of credentials")
      .securityIn(apiKeyHeader)
      .securityIn(jwtAuthHeader)
      .in("verification" / "credential")
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(jsonBody[http.VcVerificationRequests].description("List of VC to verify"))
      .out(statusCode(StatusCode.Ok).description("List of VC verification outcome"))
      .out(jsonBody[http.VcVerificationResponses])
      .errorOut(basicFailuresAndForbidden)
}
