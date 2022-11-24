package io.iohk.atala.pollux.schema

import io.iohk.atala.api.http.model.{Order, Pagination}
import io.iohk.atala.api.http.{
  FailureResponse,
  InternalServerError,
  NotFoundResponse
}
import io.iohk.atala.pollux.schema.SchemaRegistryEndpoints.{
  createSchemaEndpoint,
  getSchemaByIdEndpoint,
  lookupSchemasByQueryEndpoint
}
import io.iohk.atala.pollux.schema.model.VerifiableCredentialSchema
import io.iohk.atala.pollux.service.SchemaRegistryService
import io.iohk.atala.pollux.service.SchemaRegistryService.{
  createSchema,
  getSchemaById,
  lookupSchemas
}
import sttp.tapir.redoc.RedocUIOptions
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.{Task, URIO, ZIO, ZLayer}

import java.util.UUID

class SchemaRegistryServerEndpoints(
    schemaRegistryService: SchemaRegistryService
) {
  def throwableToInternalServerError(throwable: Throwable) =
    ZIO.fail[FailureResponse](InternalServerError(throwable.getMessage))

  // TODO: make the endpoint typed ZServerEndpoint[SchemaRegistryService, Any]
  val createSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    createSchemaEndpoint.zServerLogic(schemaInput =>
      schemaRegistryService
        .createSchema(schemaInput)
        .foldZIO(throwableToInternalServerError, schema => ZIO.succeed(schema))
    )

  val getSchemaByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getSchemaByIdEndpoint.zServerLogic(id =>
      schemaRegistryService
        .getSchemaById(id)
        .foldZIO(
          throwableToInternalServerError,
          {
            case Some(schema) => ZIO.succeed(schema)
            case None =>
              ZIO.fail[FailureResponse](
                NotFoundResponse(s"Schema is not found by $id")
              )
          }
        )
    )

  val lookupSchemasByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupSchemasByQueryEndpoint.zServerLogic {
      case (
            filter: VerifiableCredentialSchema.Filter,
            page: Pagination,
            order: Option[Order]
          ) =>
        schemaRegistryService
          .lookupSchemas(filter, page, order)
          .foldZIO(
            throwableToInternalServerError,
            pageOfVCS => ZIO.succeed(pageOfVCS)
          )
    }

  val all: List[ZServerEndpoint[Any, Any]] =
    List(
      createSchemaServerEndpoint,
      getSchemaByIdServerEndpoint,
      lookupSchemasByQueryServerEndpoint
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
