package io.iohk.atala.iam.entity.http

import io.iohk.atala.api.http.*
import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.iam.entity.http.model.*
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.{EndpointInput, PublicEndpoint, endpoint, extractFromRequest, path, query, statusCode, stringToPath}

import java.util.UUID

object EntityEndpoints {

  val createEntityEndpoint: PublicEndpoint[
    (RequestContext, CreateEntityRequest),
    ErrorResponse,
    EntityResponse,
    Any
  ] =
    endpoint.post
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
      .errorOut(basicFailures)
      .name("createEntity")
      .summary("Create a new entity record")
      .description(
        "Create the new entity record. The entity record is a representation of the account in the system."
      )
      .tag("Identity and Access Management")

  val updateEntityNameEndpoint: PublicEndpoint[
    (RequestContext, UUID, UpdateEntityNameRequest),
    ErrorResponse,
    EntityResponse,
    Any
  ] =
    endpoint.put
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
      .errorOut(basicFailuresAndNotFound)
      .name("updateEntityName")
      .summary("Update the entity record name by `id`")
      .description(
        "Update the entity record name by `id`"
      )
      .tag("Identity and Access Management")

  val updateEntityWalletIdEndpoint: PublicEndpoint[
    (RequestContext, UUID, UpdateEntityWalletIdRequest),
    ErrorResponse,
    EntityResponse,
    Any
  ] =
    endpoint.put
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "iam" / "entities" / path[UUID]("id") / "walletId" // .description(EntityResponse.annotations.id.description)
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
      .errorOut(basicFailuresAndNotFound)
      .name("updateEntityWalletId")
      .summary("Update the entity record `walletId` by `id`")
      .description(
        "Update the entity record `walletId` field by `id`"
      )
      .tag("Identity and Access Management")

  val getEntityByIdEndpoint: PublicEndpoint[
    (RequestContext, UUID),
    ErrorResponse,
    EntityResponse,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "iam" / "entities" / path[UUID]("id").description(
          "Identifier of the entity"
        )
      )
      .out(jsonBody[EntityResponse].description("Entity found by `id`"))
      .errorOut(basicFailuresAndNotFound)
      .name("getEntityById")
      .summary("Get the entity by the `id`")
      .description(
        "Get the entity by the unique identifier"
      )
      .tag("Identity and Access Management")

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]
  val getEntitiesEndpoint: PublicEndpoint[
    (
        RequestContext,
        PaginationInput
    ),
    ErrorResponse,
    EntityResponsePage,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("iam" / "entities".description("Get all entities"))
      .in(paginationInput)
      .out(jsonBody[EntityResponsePage].description("Collection of Entity records"))
      .errorOut(basicFailures)
      .name("getAllEntities")
      .summary("Get all entities")
      .description(
        "Get all entities with the pagination by `offset` and `limit` parameters "
      )
      .tag("Identity and Access Management")

  val deleteEntityByIdEndpoint: PublicEndpoint[
    (RequestContext, UUID),
    ErrorResponse,
    Unit,
    Any
  ] =
    endpoint.delete
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "iam" / "entities" / path[UUID]("id").description(
          "Identifier of the entity"
        )
      )
      .out(
        statusCode(StatusCode.Ok).description("Entity deleted successfully")
      )
      .errorOut(basicFailuresAndNotFound)
      .name("deleteEntityById")
      .summary("Delete the entity by `id`")
      .description(
        "Delete the entity by the unique identifier"
      )
      .tag("Identity and Access Management")
}
