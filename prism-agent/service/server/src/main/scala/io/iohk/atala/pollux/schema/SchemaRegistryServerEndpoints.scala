package io.iohk.atala.pollux.schema

import io.iohk.atala.pollux.service.SchemaRegistryService.{createSchema, getSchemaById}
import io.iohk.atala.pollux.service.SchemaRegistryService
import sttp.tapir.redoc.RedocUIOptions
import sttp.tapir.ztapir.RichZServerEndpoint
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.{Task, URIO, ZIO, ZLayer}
import sttp.tapir.ztapir.ZServerEndpoint
import io.iohk.atala.api.http.NotFoundResponse
import SchemaRegistryEndpoints.{createSchemaEndpoint, getSchemaByIdEndpoint}

import java.util.UUID

class SchemaRegistryServerEndpoints(schemaRegistryService: SchemaRegistryService) {
  val createSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    createSchemaEndpoint.serverLogicSuccess(schemaInput => schemaRegistryService.createSchema(schemaInput))

  val getSchemaByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getSchemaByIdEndpoint.serverLogic(id =>
      schemaRegistryService.getSchemaById(id).map {
        case Some(schema) => Right(schema)
        case None         => Left(NotFoundResponse("Schema is not found by id"))
      }
    )

  val all: List[ZServerEndpoint[Any, Any]] = List(createSchemaServerEndpoint, getSchemaByIdServerEndpoint)
}

object SchemaRegistryServerEndpoints {
  def all: URIO[SchemaRegistryService, List[ZServerEndpoint[Any, Any]]] = {
    for {
      schemaRegistryService <- ZIO.service[SchemaRegistryService]
      schemaRegistryEndpoints = new SchemaRegistryServerEndpoints(schemaRegistryService)
    } yield schemaRegistryEndpoints.all
  }
}
