package io.iohk.atala.iam.entity.http

import io.iohk.atala.api.http.*
import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.iam.authentication.admin.AdminApiKeyCredentials
import io.iohk.atala.iam.authentication.admin.AdminApiKeySecurityLogic.adminApiKeyHeader
import io.iohk.atala.iam.entity.http.model.*
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.{Endpoint, EndpointInput, endpoint, extractFromRequest, path, query, statusCode, stringToPath}

import java.util.UUID

object EntityEndpoints {

  val createEntityEndpoint: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, CreateEntityRequest),
    ErrorResponse,
    EntityResponse,
    Any
  ] =
    endpoint.post
      .securityIn(adminApiKeyHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("iam" / "entities")
      .in(
        jsonBody[CreateEntityRequest]
          .description(
            "JSON object required for the entity creation"
          )
      )
      .out(
        statusCode(StatusCode.Created)
          .description(
            "The new entity is successfully created"
          )
      )
      .out(jsonBody[EntityResponse])
      .description("Entity record")
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .name("createEntity")
      .summary("Create a new entity record")
      .description(
        "Create the new entity record. The entity record is a representation of the account in the system."
      )
      .tag("Identity and Access Management")

  val updateEntityNameEndpoint: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, UUID, UpdateEntityNameRequest),
    ErrorResponse,
    EntityResponse,
    Any
  ] =
    endpoint.put
      .securityIn(adminApiKeyHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "iam" / "entities" / path[UUID]("id") / "name" // .description(EntityResponse.annotations.id.description)
      )
      .in(
        jsonBody[UpdateEntityNameRequest]
          .description(
            "JSON object required for the entity name update"
          )
      )
      .out(
        statusCode(StatusCode.Ok)
          .description(
            "The entity record is successfully updated"
          )
      )
      .out(jsonBody[EntityResponse])
      .description("Entity record")
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .name("updateEntityName")
      .summary("Update the entity record name by `id`")
      .description(
        "Update the entity record name by `id`"
      )
      .tag("Identity and Access Management")

  val updateEntityWalletIdEndpoint: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, UUID, UpdateEntityWalletIdRequest),
    ErrorResponse,
    EntityResponse,
    Any
  ] =
    endpoint.put
      .securityIn(adminApiKeyHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "iam" / "entities" / path[UUID]("id") / "walletId"
      )
      .in(
        jsonBody[UpdateEntityWalletIdRequest]
          .description(
            "JSON object required for the entity walletId update"
          )
      )
      .out(
        statusCode(StatusCode.Ok)
          .description(
            "The entity record is successfully updated"
          )
      )
      .out(jsonBody[EntityResponse])
      .description("Entity record")
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .name("updateEntityWalletId")
      .summary("Update the entity record `walletId` by `id`")
      .description(
        "Update the entity record `walletId` field by `id`"
      )
      .tag("Identity and Access Management")

  val getEntityByIdEndpoint: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, UUID),
    ErrorResponse,
    EntityResponse,
    Any
  ] =
    endpoint.get
      .securityIn(adminApiKeyHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "iam" / "entities" / path[UUID]("id").description(
          "Identifier of the entity"
        )
      )
      .out(jsonBody[EntityResponse].description("Entity found by `id`"))
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .name("getEntityById")
      .summary("Get the entity by the `id`")
      .description(
        "Get the entity by the unique identifier"
      )
      .tag("Identity and Access Management")

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]
  val getEntitiesEndpoint: Endpoint[
    AdminApiKeyCredentials,
    (
        RequestContext,
        PaginationInput
    ),
    ErrorResponse,
    EntityResponsePage,
    Any
  ] =
    endpoint.get
      .securityIn(adminApiKeyHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("iam" / "entities".description("Get all entities"))
      .in(paginationInput)
      .out(jsonBody[EntityResponsePage].description("Collection of Entity records"))
      .errorOut(basicFailuresAndForbidden)
      .name("getAllEntities")
      .summary("Get all entities")
      .description(
        "Get all entities with the pagination by `offset` and `limit` parameters "
      )
      .tag("Identity and Access Management")

  val deleteEntityByIdEndpoint: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, UUID),
    ErrorResponse,
    Unit,
    Any
  ] =
    endpoint.delete
      .securityIn(adminApiKeyHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "iam" / "entities" / path[UUID]("id").description(
          "Identifier of the entity"
        )
      )
      .out(
        statusCode(StatusCode.Ok).description("Entity deleted successfully")
      )
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .name("deleteEntityById")
      .summary("Delete the entity by `id`")
      .description(
        "Delete the entity by the unique identifier"
      )
      .tag("Identity and Access Management")

  val addEntityApiKeyAuthenticationEndpoint: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, ApiKeyAuthenticationRequest),
    ErrorResponse,
    Unit,
    Any
  ] = endpoint.post
    .securityIn(adminApiKeyHeader)
    .in(extractFromRequest[RequestContext](RequestContext.apply))
    .in("iam" / "apikey-authentication")
    .in(
      jsonBody[ApiKeyAuthenticationRequest]
        .description(
          "JSON object required for the registering the entity and `apikey`"
        )
    )
    .out(
      statusCode(StatusCode.Created)
        .description(
          "The new `apikey` is successfully registered for the entity"
        )
    )
    .errorOut(basicFailureAndNotFoundAndForbidden)
    .name("addEntityApiKeyAuthentication")
    .summary("Register the `apikey` for the entity")
    .description(
      "Register the `apikey` for the entity."
    )
    .tag("Identity and Access Management")

  val deleteEntityApiKeyAuthenticationEndpoint: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, ApiKeyAuthenticationRequest),
    ErrorResponse,
    Unit,
    Any
  ] = endpoint.delete
    .securityIn(adminApiKeyHeader)
    .in(extractFromRequest[RequestContext](RequestContext.apply))
    .in("iam" / "apikey-authentication")
    .in(
      jsonBody[ApiKeyAuthenticationRequest]
        .description(
          "JSON object required for the unregistering the entity and `apikey`"
        )
    )
    .out(
      statusCode(StatusCode.Ok)
        .description(
          "The new `apikey` is successfully unregistered for the entity"
        )
    )
    .errorOut(basicFailureAndNotFoundAndForbidden)
    .name("deleteEntityApiKeyAuthentication")
    .summary("Unregister the `apikey` for the entity")
    .description(
      "Unregister the `apikey` for the entity."
    )
    .tag("Identity and Access Management")
}
