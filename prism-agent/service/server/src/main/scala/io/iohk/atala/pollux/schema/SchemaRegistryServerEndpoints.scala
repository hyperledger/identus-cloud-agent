package io.iohk.atala.pollux.schema

import io.iohk.atala.pollux.service.SchemaRegistryService.{
  createSchema,
  getSchemaById
}
import io.iohk.atala.pollux.service.SchemaRegistryService
import sttp.tapir.redoc.RedocUIOptions
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.{Task, URIO, ZIO, ZLayer}
import io.iohk.atala.api.http.{
  FailureResponse,
  InternalServerError,
  NotFoundResponse
}
import SchemaRegistryEndpoints.{createSchemaEndpoint, getSchemaByIdEndpoint}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.ZServerEndpoint
import sttp.tapir.ztapir.*

import java.util.UUID

class SchemaRegistryServerEndpoints(
    schemaRegistryService: SchemaRegistryService
) {
  val createSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    createSchemaEndpoint.zServerLogic(schemaInput =>
      schemaRegistryService
        .createSchema(schemaInput)
        .foldZIO(
          throwable => ZIO.fail[FailureResponse](InternalServerError(throwable.getMessage)),
          schema => ZIO.succeed(schema)
        )
    )

  val getSchemaByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getSchemaByIdEndpoint.zServerLogic(id =>
      schemaRegistryService
        .getSchemaById(id)
        .foldZIO(
          throwable => ZIO.fail[FailureResponse](InternalServerError(throwable.getMessage)),
          {
            case Some(schema) => ZIO.succeed(schema)
            case None => ZIO.fail[FailureResponse](NotFoundResponse(s"Schema is not found by $id"))
          }
        )
    )

  val all: List[ZServerEndpoint[Any, Any]] =
    List(createSchemaServerEndpoint, getSchemaByIdServerEndpoint)
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
