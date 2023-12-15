package io.iohk.atala.agent.server.http

import sttp.apispec.SecurityScheme
import sttp.apispec.openapi.{OpenAPI, Server}
import sttp.model.headers.AuthenticationScheme
import sttp.tapir.redoc.RedocUIOptions
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.collection.immutable.ListMap

object ZHttpEndpoints {

  private val swaggerUIOptions = SwaggerUIOptions.default
    .contextPath(List("docs", "prism-agent", "api"))

  private val redocUIOptions = RedocUIOptions.default
    .copy(pathPrefix = List("redoc"))

  def swaggerEndpoints[F[_]](apiEndpoints: List[ServerEndpoint[Any, F]]): List[ServerEndpoint[Any, F]] =
    SwaggerInterpreter(swaggerUIOptions = swaggerUIOptions, customiseDocsModel = DocModels.customiseDocsModel)
      .fromServerEndpoints[F](apiEndpoints, "Prism Agent", "1.0.0")

  def redocEndpoints[F[_]](apiEndpoints: List[ServerEndpoint[Any, F]]): List[ServerEndpoint[Any, F]] =
    RedocInterpreter(redocUIOptions = redocUIOptions, customiseDocsModel = DocModels.customiseDocsModel)
      .fromServerEndpoints[F](apiEndpoints, "Prism Agent", "1.0.0")

  def withDocumentations[F[_]](apiEndpoints: List[ServerEndpoint[Any, F]]): List[ServerEndpoint[Any, F]] = {
    apiEndpoints ++ swaggerEndpoints[F](apiEndpoints) ++ redocEndpoints[F](apiEndpoints)
  }
}
