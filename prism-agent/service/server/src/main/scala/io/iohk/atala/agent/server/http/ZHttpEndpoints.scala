package io.iohk.atala.agent.server.http

import zio.{Task, ZIO, ZLayer, URIO}
import io.iohk.atala.pollux.schema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.service.SchemaRegistryService
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.{RichZServerEndpoint, ZServerEndpoint}
import sttp.tapir.redoc.RedocUIOptions

object ZHttpEndpoints {
  def swaggerEndpoints(apiEndpoints: List[ZServerEndpoint[Any, Any]]): List[ZServerEndpoint[Any, Any]] =
    SwaggerInterpreter().fromServerEndpoints[Task](apiEndpoints, "Prism Agent", "1.0.0")

  def redocEndpoints(apiEndpoints: List[ZServerEndpoint[Any, Any]]): List[ZServerEndpoint[Any, Any]] =
    RedocInterpreter(redocUIOptions = RedocUIOptions.default.copy(pathPrefix = List("redoc")))
      .fromServerEndpoints[Task](apiEndpoints, "Prism Agent", "1.0.0")

  def withDocumentations(apiEndpoints: List[ZServerEndpoint[Any, Any]]): List[ZServerEndpoint[Any, Any]] = {
    apiEndpoints ++ swaggerEndpoints(apiEndpoints) ++ redocEndpoints(apiEndpoints)
  }
}
