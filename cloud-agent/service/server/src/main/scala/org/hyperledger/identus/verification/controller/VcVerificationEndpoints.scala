package org.hyperledger.identus.verification.controller

import org.hyperledger.identus.api.http.EndpointOutputs.*
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.iam.authentication.apikey.ApiKeyCredentials
import org.hyperledger.identus.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import org.hyperledger.identus.iam.authentication.oidc.JwtCredentials
import org.hyperledger.identus.iam.authentication.oidc.JwtSecurityLogic.jwtAuthHeader
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

object VcVerificationEndpoints {
  val verify: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, List[http.VcVerificationRequest]),
    ErrorResponse,
    List[http.VcVerificationResponse],
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
      .in(jsonBody[List[http.VcVerificationRequest]].description("List of VC to verify"))
      .out(statusCode(StatusCode.Ok).description("List of VC verification outcome"))
      .out(jsonBody[List[http.VcVerificationResponse]])
      .errorOut(basicFailuresAndForbidden)
}
