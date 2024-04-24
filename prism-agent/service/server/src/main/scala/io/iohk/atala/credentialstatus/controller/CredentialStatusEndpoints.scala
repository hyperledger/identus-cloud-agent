package io.iohk.atala.credentialstatus.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.credentialstatus.controller.http.StatusListCredential
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import io.iohk.atala.iam.authentication.oidc.JwtSecurityLogic.jwtAuthHeader
import java.util.UUID
import org.hyperledger.identus.pollux.core.model.DidCommID
import io.iohk.atala.api.http.codec.DidCommIDCodec.given
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
object CredentialStatusEndpoints {

  val getCredentialStatusListEndpoint: PublicEndpoint[
    (RequestContext, UUID),
    ErrorResponse,
    StatusListCredential,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "credential-status" / path[UUID]("id").description(
          "Globally unique identifier of the credential status list"
        )
      )
      .out(jsonBody[StatusListCredential].description("Status List credential with embedded proof found by ID"))
      .errorOut(basicFailuresAndNotFound)
      .name("getCredentialStatusListEndpoint")
      .summary("Fetch credential status list by its ID")
      .description(
        "Fetch credential status list by its ID"
      )
      .tag("Credential status list")

  val revokeCredentialByIdEndpoint: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, DidCommID),
    ErrorResponse,
    Unit,
    Any
  ] =
    endpoint.patch
      .securityIn(apiKeyHeader)
      .securityIn(jwtAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "credential-status" / "revoke-credential" / path[DidCommID]("id").description("Revoke a credential by its ID")
      )
      .out(statusCode(sttp.model.StatusCode.Ok))
      .errorOut(basicFailuresAndNotFound)
      .summary("Revoke a credential by its ID")
      .description("Marks credential to be ready for revocation, it will be revoked automatically")
      .tag("Credential status list")
}
