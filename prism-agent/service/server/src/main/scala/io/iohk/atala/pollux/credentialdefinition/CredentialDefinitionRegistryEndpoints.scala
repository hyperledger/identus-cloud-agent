package io.iohk.atala.pollux.credentialdefinition

import io.iohk.atala.api.http.*
import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.api.http.codec.OrderCodec.*
import io.iohk.atala.api.http.model.{Order, PaginationInput}
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.iam.authentication.oidc.JwtSecurityLogic.bearerAuthHeader
import io.iohk.atala.pollux.credentialdefinition.http.{ CredentialDefinitionInput, CredentialDefinitionResponse, CredentialDefinitionResponsePage, FilterInput }
import sttp.model.StatusCode
import sttp.tapir.json.zio.{jsonBody, schemaForZioJsonValue}
import sttp.tapir.{ Endpoint, EndpointInput, PublicEndpoint, endpoint, extractFromRequest, path, query, statusCode, stringToPath }

import java.util.UUID

object CredentialDefinitionRegistryEndpoints {

  val createCredentialDefinitionEndpoint: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, CredentialDefinitionInput),
    ErrorResponse,
    CredentialDefinitionResponse,
    Any
  ] =
    endpoint.post
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("credential-definition-registry" / "definitions")
      .in(
        jsonBody[CredentialDefinitionInput]
          .description(
            "JSON object required for the credential definition creation"
          )
      )
      .out(
        statusCode(StatusCode.Created)
          .description(
            "The new credential definition record is successfully created"
          )
      )
      .out(jsonBody[http.CredentialDefinitionResponse])
      .description("Credential definition record")
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .name("createCredentialDefinition")
      .summary("Publish new definition to the definition registry")
      .description(
        "Create the new credential definition record with metadata and internal JSON Schema on behalf of Cloud Agent. " +
          "The credential definition will be signed by the keys of Cloud Agent and issued by the DID that corresponds to it."
      )
      .tag("Credential Definition Registry")

  val getCredentialDefinitionByIdEndpoint: PublicEndpoint[
    (RequestContext, UUID),
    ErrorResponse,
    CredentialDefinitionResponse,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "credential-definition-registry" / "definitions" / path[UUID]("guid").description(
          "Globally unique identifier of the credential definition record"
        )
      )
      .out(jsonBody[CredentialDefinitionResponse].description("CredentialDefinition found by `guid`"))
      .errorOut(basicFailuresAndNotFound)
      .name("getCredentialDefinitionById")
      .summary("Fetch the credential definition from the registry by `guid`")
      .description(
        "Fetch the credential definition by the unique identifier"
      )
      .tag("Credential Definition Registry")

  val getCredentialDefinitionInnerDefinitionByIdEndpoint: PublicEndpoint[
    (RequestContext, UUID),
    ErrorResponse,
    zio.json.ast.Json,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "credential-definition-registry" / "definitions" / path[UUID]("guid") / "definition".description(
          "Globally unique identifier of the credential definition record"
        )
      )
      .out(jsonBody[zio.json.ast.Json].description("CredentialDefinition found by `guid`"))
      .errorOut(basicFailuresAndNotFound)
      .name("getCredentialDefinitionInnerDefinitionById")
      .summary("Fetch the inner definition field of the credential definition from the registry by `guid`")
      .description(
        "Fetch the inner definition fields of the credential definition by the unique identifier"
      )
      .tag("Credential Definition Registry")

  private val credentialDefinitionFilterInput: EndpointInput[http.FilterInput] = EndpointInput.derived[http.FilterInput]
  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]
  val lookupCredentialDefinitionsByQueryEndpoint: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (
        RequestContext,
        FilterInput,
        PaginationInput,
        Option[Order]
    ),
    ErrorResponse,
    CredentialDefinitionResponsePage,
    Any
  ] =
    endpoint.get
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "credential-definition-registry" / "definitions".description(
          "Lookup credential definitions by query"
        )
      )
      .in(credentialDefinitionFilterInput)
      .in(paginationInput)
      .in(query[Option[Order]]("order"))
      .out(jsonBody[CredentialDefinitionResponsePage].description("Collection of CredentialDefinitions records."))
      .errorOut(basicFailures)
      .name("lookupCredentialDefinitionsByQuery")
      .summary("Lookup credential definitions by indexed fields")
      .description(
        "Lookup credential definitions by `author`, `name`, `tag` parameters and control the pagination by `offset` and `limit` parameters "
      )
      .tag("Credential Definition Registry")
}
