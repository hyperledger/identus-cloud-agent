package io.iohk.atala.pollux.stub

import io.iohk.atala.pollux.models.NotFoundResponse
import io.iohk.atala.pollux.services.SchemaServiceInMemory
import sttp.tapir.redoc.RedocUIOptions
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.Task
import sttp.tapir.ztapir.{ZServerEndpoint}

// Here we bind particular endpoint definition to the controller layer that is able to 
// - get the INPUT
// - call the service layer
// - transform the result to the OUTPUT
// Swagger UI routes and Redoc routes are defined here as well
object SchemaEndpointsInMemory {

  val createSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    io.iohk.atala.pollux.api.VerifiableCredentialsSchemaEndpoints.createSchemaEndpoint.serverLogicSuccess(schemaInput =>
      SchemaServiceInMemory.instance.createSchema(schemaInput)
    )

  val getSchemaByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    io.iohk.atala.pollux.api.VerifiableCredentialsSchemaEndpoints.getSchemaById.serverLogic(id =>
      SchemaServiceInMemory.instance.getSchemaById(id).map {
        case Some(schema) => Right(schema)
        case None         => Left(NotFoundResponse("Schema is not found by id"))
      }
    )

  val apiEndpoints: List[ZServerEndpoint[Any, Any]] = List(createSchemaServerEndpoint, getSchemaByIdServerEndpoint)

  val docEndpoints: List[ZServerEndpoint[Any, Any]] = SwaggerInterpreter()
    .fromServerEndpoints[Task](apiEndpoints, "pollux", "1.0.0")

  val redocEndpoints: List[ZServerEndpoint[Any, Any]] =
    RedocInterpreter(redocUIOptions = RedocUIOptions.default.copy(pathPrefix = List("redoc")))
      .fromServerEndpoints[Task](apiEndpoints, "pollux", "1.0.0")

  val all: List[ZServerEndpoint[Any, Any]] = apiEndpoints ++ docEndpoints ++ redocEndpoints
}
