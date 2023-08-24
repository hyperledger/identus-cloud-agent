package io.iohk.atala.agent.server.http

import sttp.apispec.openapi.{OpenAPI, Server}
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.redoc.RedocUIOptions
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.SwaggerUIOptions

object ZHttpEndpoints {

  val swaggerUIOptions = SwaggerUIOptions.default
    .contextPath(List("docs", "prism-agent", "api"))

  val customiseDocsModel: OpenAPI => OpenAPI = { oapi =>
    oapi.servers(
      List(
        Server(url = "http://localhost:8085", description = Some("Local Prism Agent")),
        Server(url = "http://localhost/prism-agent", description = Some("Local Prism Agent with APISIX proxy")),
        Server(
          url = "https://k8s-dev.atalaprism.io/prism-agent",
          description = Some("Prism Agent on the Staging Environment")
        ),
      )
    )
  }

  def swaggerEndpoints[F[_]](apiEndpoints: List[ServerEndpoint[Any, F]]): List[ServerEndpoint[Any, F]] =
    SwaggerInterpreter(swaggerUIOptions = swaggerUIOptions, customiseDocsModel = customiseDocsModel)
      .fromServerEndpoints[F](apiEndpoints, "Prism Agent", "1.0.0")

  def redocEndpoints[F[_]](apiEndpoints: List[ServerEndpoint[Any, F]]): List[ServerEndpoint[Any, F]] =
    RedocInterpreter(redocUIOptions = RedocUIOptions.default.copy(pathPrefix = List("redoc")))
      .fromServerEndpoints[F](apiEndpoints, title = "Prism Agent", version = "1.0.0")

  def withDocumentations[F[_]](apiEndpoints: List[ServerEndpoint[Any, F]]): List[ServerEndpoint[Any, F]] = {
    apiEndpoints ++ swaggerEndpoints[F](apiEndpoints) ++ redocEndpoints[F](apiEndpoints)
  }
}
