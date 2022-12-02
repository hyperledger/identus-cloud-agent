package io.iohk.atala.pollux.schema

import io.iohk.atala.api.http.model.{CollectionStats, Order, Pagination, PaginationInput}
import io.iohk.atala.api.http.{FailureResponse, InternalServerError, NotFoundResponse, RequestContext}
import io.iohk.atala.pollux.schema.SchemaRegistryEndpoints.{
  createSchemaEndpoint,
  getSchemaByIdEndpoint,
  lookupSchemasByQueryEndpoint,
  testEndpoint
}
import io.iohk.atala.pollux.schema.model.{
  VerifiableCredentialSchema,
  VerifiableCredentialSchemaInput,
  VerifiableCredentialSchemaPage
}
import io.iohk.atala.pollux.service.SchemaRegistryService
import io.iohk.atala.pollux.service.SchemaRegistryService.{createSchema, getSchemaById, lookupSchemas}
import sttp.tapir.redoc.RedocUIOptions
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.{Task, URIO, ZIO, ZLayer}
import io.iohk.atala.pollux.schema.controller.SchemaRegistryController

import java.util.UUID

class SchemaRegistryServerEndpoints(
    schemaRegistryService: SchemaRegistryService
) {
  def throwableToInternalServerError(throwable: Throwable) =
    ZIO.fail[FailureResponse](InternalServerError(throwable.getMessage))

  // TODO: make the endpoint typed ZServerEndpoint[SchemaRegistryService, Any]
  val createSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    createSchemaEndpoint.zServerLogic {
      case (
            ctx: RequestContext,
            schemaInput: VerifiableCredentialSchemaInput
          ) =>
        schemaRegistryService
          .createSchema(schemaInput)
          .foldZIO(
            throwableToInternalServerError,
            schema =>
              ZIO.succeed(
                schema.withBaseUri(ctx.request.uri)
              )
          )
    }

  val getSchemaByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getSchemaByIdEndpoint.zServerLogic { case (ctx: RequestContext, id: UUID) =>
      schemaRegistryService
        .getSchemaById(id)
        .foldZIO(
          throwableToInternalServerError,
          {
            case Some(schema) =>
              ZIO.succeed(schema.withSelf(ctx.request.uri.toString))
            case None =>
              ZIO.fail[FailureResponse](
                NotFoundResponse(s"Schema is not found by $id")
              )
          }
        )
    }

  val lookupSchemasByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupSchemasByQueryEndpoint.zServerLogic {
      case (
            ctx: RequestContext,
            filter: VerifiableCredentialSchema.Filter,
            paginationInput: PaginationInput,
            order: Option[Order]
          ) =>
        schemaRegistryService
          .lookupSchemas(filter, paginationInput.toPagination, order)
          .foldZIO(
            throwableToInternalServerError,
            {
              case (
                    page: VerifiableCredentialSchemaPage,
                    stats: CollectionStats
                  ) =>
                ZIO.succeed(
                  SchemaRegistryController(
                    ctx,
                    paginationInput.toPagination,
                    page,
                    stats
                  ).result
                )
            }
          )
    }

  val testServerEndpoint: ZServerEndpoint[Any, Any] =
    testEndpoint.zServerLogic(requestContext => ZIO.succeed(requestContext.request.toString))

  val all: List[ZServerEndpoint[Any, Any]] =
    List(
      createSchemaServerEndpoint,
      getSchemaByIdServerEndpoint,
      lookupSchemasByQueryServerEndpoint,
      testServerEndpoint
    )
}

object SchemaRegistryServerEndpoints {
  def all: URIO[SchemaRegistryService, List[ZServerEndpoint[Any, Any]]] = {
    for {
      schemaRegistryService <- ZIO.service[SchemaRegistryService]
      schemaRegistryEndpoints = new SchemaRegistryServerEndpoints(
        schemaRegistryService
      )
    } yield schemaRegistryEndpoints.all
  }
}
