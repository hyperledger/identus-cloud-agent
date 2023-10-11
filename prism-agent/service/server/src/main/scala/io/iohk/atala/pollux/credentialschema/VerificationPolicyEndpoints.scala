package io.iohk.atala.pollux.credentialschema

import io.iohk.atala.api.http.*
import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.api.http.codec.OrderCodec.*
import io.iohk.atala.api.http.model.{Order, PaginationInput}
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.iam.authentication.oidc.JwtSecurityLogic.bearerAuthHeader
import io.iohk.atala.pollux.credentialschema.http.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

import java.util.UUID

object VerificationPolicyEndpoints {

  val createVerificationPolicyEndpoint: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, VerificationPolicyInput),
    ErrorResponse,
    VerificationPolicy,
    Any
  ] = endpoint.post
    .securityIn(apiKeyHeader)
    .securityIn(bearerAuthHeader)
    .in(extractFromRequest[RequestContext](RequestContext.apply))
    .in("verification" / "policies")
    .in(
      jsonBody[VerificationPolicyInput].description(
        "Create verification policy object"
      )
    )
    .out(
      statusCode(StatusCode.Created).description(
        "Verification policy successfully created"
      )
    )
    .out(
      jsonBody[VerificationPolicy].description(
        "Created verification policy entity"
      )
    )
    .errorOut(basicFailuresAndForbidden)
    .name("createVerificationPolicy")
    .summary("Create the new verification policy")
    .description("Create the new verification policy")
    .tag("Verification")

  val updateVerificationPolicyEndpoint: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, UUID, Int, VerificationPolicyInput),
    ErrorResponse,
    VerificationPolicy,
    Any
  ] =
    endpoint.put
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("verification" / "policies" / path[UUID]("id"))
      .in(
        query[Int](name = "nonce").description(
          "Nonce of the previous VerificationPolicy"
        )
      )
      .in(
        jsonBody[VerificationPolicyInput].description(
          "Update verification policy object"
        )
      )
      .out(statusCode(StatusCode.Ok))
      .out(jsonBody[VerificationPolicy])
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .name("updateVerificationPolicy")
      .summary("Update the verification policy object by id")
      .description(
        "Update the verification policy entry"
      )
      .tag("Verification")

  val getVerificationPolicyByIdEndpoint: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, UUID),
    ErrorResponse,
    VerificationPolicy,
    Any
  ] =
    endpoint.get
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "verification" / "policies" / path[UUID]("id")
          .description("Get the verification policy by id")
      )
      .out(jsonBody[VerificationPolicy])
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .name("getVerificationPolicyById")
      .summary("Fetch the verification policy by id")
      .description(
        "Get the verification policy by id"
      )
      .tag("Verification")

  val deleteVerificationPolicyByIdEndpoint: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, UUID),
    ErrorResponse,
    Unit,
    Any
  ] =
    endpoint.delete
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "verification" / "policies" / path[UUID]("id")
          .description("Delete the verification policy by id")
      )
      .out(
        statusCode(StatusCode.Ok).description(
          "Verification policy deleted successfully"
        )
      )
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .name("deleteVerificationPolicyById")
      .summary("Deleted the verification policy by id")
      .description(
        "Delete the verification policy by id"
      )
      .tag("Verification")

  val lookupVerificationPoliciesByQueryEndpoint: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, VerificationPolicy.Filter, PaginationInput, Option[Order]),
    ErrorResponse,
    VerificationPolicyPage,
    Any
  ] =
    endpoint.get
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "verification" / "policies"
          .description("Lookup verification policy by query")
      )
      .in(query[Option[String]]("name").mapTo[VerificationPolicy.Filter])
      .in(
        query[Option[Int]]("offset")
          .and(query[Option[Int]]("limit"))
          .mapTo[PaginationInput]
      )
      .in(query[Option[Order]]("order"))
      .out(jsonBody[VerificationPolicyPage])
      .errorOut(basicFailuresAndForbidden)
      .name("lookupVerificationPoliciesByQuery")
      .summary("Lookup verification policies by query")
      .description(
        "Lookup verification policies by `name`, and control the pagination by `offset` and `limit` parameters"
      )
      .tag("Verification")
}
