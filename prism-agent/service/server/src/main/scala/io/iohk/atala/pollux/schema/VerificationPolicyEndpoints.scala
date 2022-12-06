package io.iohk.atala.pollux.schema

import io.iohk.atala.api.http.codec.OrderCodec.*
import io.iohk.atala.api.http.model.{Order, PaginationInput}
import io.iohk.atala.api.http.{BadRequest, FailureResponse, InternalServerError, NotFound, RequestContext}
import io.iohk.atala.pollux.schema.model.{
  VerifiableCredentialSchema,
  VerifiableCredentialSchemaInput,
  VerifiableCredentialSchemaPage
}
import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.pollux.schema.model.{
  VerifiableCredentialSchema,
  VerificationPolicy,
  VerificationPolicyInput,
  VerificationPolicyPage
}
import sttp.model.StatusCode
import sttp.tapir.EndpointIO.Info
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.{
  Endpoint,
  EndpointInfo,
  PublicEndpoint,
  endpoint,
  extractFromRequest,
  oneOf,
  oneOfDefaultVariant,
  oneOfVariant,
  path,
  query,
  statusCode,
  stringToPath
}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

import java.util.UUID

object VerificationPolicyEndpoints {

  val createVerificationPolicyEndpoint: PublicEndpoint[
    (RequestContext, VerificationPolicyInput),
    FailureResponse,
    VerificationPolicy,
    Any
  ] =
    endpoint.post
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("verification" / "policies")
      .in(
        jsonBody[VerificationPolicyInput].description(
          "Create verification policy object"
        )
      )
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[VerificationPolicy])
      .errorOut(basicFailures)
      .name("createVerificationPolicy")
      .summary("Create the new verification policy")
      .description("Create the new verification policy")
      .tag("Verification")

  val updateVerificationPolicyEndpoint: PublicEndpoint[
    (RequestContext, String, VerificationPolicyInput),
    FailureResponse,
    VerificationPolicy,
    Any
  ] =
    endpoint.put
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("verification" / "policies" / path[String]("id"))
      .in(
        jsonBody[VerificationPolicyInput].description(
          "Update verification policy object"
        )
      )
      .out(statusCode(StatusCode.Ok))
      .out(jsonBody[VerificationPolicy])
      .errorOut(basicFailuresAndNotFound)
      .name("updateVerificationPolicy")
      .summary("Update the verification policy object by id")
      .description(
        "Update the fields of the verification policy entry: `attributes`, `issuerDIDs`, `name`, `credentialTypes`, "
      )
      .tag("Verification")

  val getVerificationPolicyByIdEndpoint: PublicEndpoint[
    (RequestContext, String),
    FailureResponse,
    VerificationPolicy,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "verification" / "policies" / path[String]("id")
          .description("Get the verification policy by id")
      )
      .out(jsonBody[VerificationPolicy])
      .errorOut(basicFailuresAndNotFound)
      .name("getVerificationPolicyById")
      .summary("Fetch the verification policy by id")
      .description(
        "Get the verification policy by id"
      )
      .tag("Verification")

  val deleteVerificationPolicyByIdEndpoint: PublicEndpoint[
    (RequestContext, String),
    FailureResponse,
    Unit,
    Any
  ] =
    endpoint.delete
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "verification" / "policies" / path[String]("id")
          .description("Delete the verification policy by id")
      )
      .out(statusCode(StatusCode.Ok))
      .errorOut(basicFailuresAndNotFound)
      .name("deleteVerificationPolicyById")
      .summary("Deleted the verification policy by id")
      .description(
        "Delete the verification policy by id"
      )
      .tag("Verification")

  val lookupVerificationPoliciesByQueryEndpoint: PublicEndpoint[
    (RequestContext, VerificationPolicy.Filter, PaginationInput, Option[Order]),
    FailureResponse,
    VerificationPolicyPage,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "verification" / "policies"
          .description("Lookup verification policy by query")
      )
      .in(
        query[Option[String]]("name")
          .and(
            query[Option[String]]("attributes")
              .and(
                query[Option[String]]("issuerDIDs")
                  .and(
                    query[Option[String]]("credentialTypes")
                  )
              )
          )
          .mapTo[VerificationPolicy.Filter]
      )
      .in(
        query[Option[Int]]("offset")
          .and(query[Option[Int]]("limit"))
          .mapTo[PaginationInput]
      )
      .in(query[Option[Order]]("order"))
      .out(jsonBody[VerificationPolicyPage])
      .errorOut(basicFailures)
      .name("lookupVerificationPoliciesByQuery")
      .summary("Lookup verification policies by query")
      .description(
        "Lookup verification policies by `name`, `attributes`, `issuerDIDs`, and `credentialTypes` and control the pagination by `offset` and `limit` parameters"
      )
      .tag("Verification")
}
