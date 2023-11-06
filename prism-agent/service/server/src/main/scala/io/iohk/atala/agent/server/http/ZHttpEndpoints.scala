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

  val swaggerUIOptions = SwaggerUIOptions.default
    .contextPath(List("docs", "prism-agent", "api"))

  val customiseDocsModel: OpenAPI => OpenAPI = { oapi =>
    oapi
      .servers(
        List(
          Server(url = "http://localhost:8085", description = Some("Local Prism Agent")),
          Server(url = "http://localhost/prism-agent", description = Some("Local Prism Agent with APISIX proxy")),
          Server(
            url = "https://k8s-dev.atalaprism.io/prism-agent",
            description = Some("Prism Agent on the Staging Environment")
          ),
        )
      )
      .components(
        oapi.components
          .getOrElse(sttp.apispec.openapi.Components.Empty)
          .copy(securitySchemes =
            ListMap(
              "apiKeyAuth" -> Right(apiKeySecuritySchema),
              "adminApiKeyAuth" -> Right(adminApiKeySecuritySchema),
              "jwtAuth" -> Right(jwtSecurityScheme)
            )
          )
      )
      .addSecurity(
        ListMap(
          "apiKeyAuth" -> Vector.empty[String],
          "adminApiKeyAuth" -> Vector.empty[String],
          "jwtAuth" -> Vector.empty[String]
        )
      )

  }

  private val apiKeySecuritySchema = SecurityScheme(
    `type` = "apiKey",
    description = Some("API Key Authentication. The header `apikey` must be set with the API key."),
    name = Some("apikey"),
    in = Some("header"),
    scheme = None,
    bearerFormat = None,
    flows = None,
    openIdConnectUrl = None
  )

  private val adminApiKeySecuritySchema = SecurityScheme(
    `type` = "apiKey",
    description =
      Some("Admin API Key Authentication. The header `x-admin-api-key` must be set with the Admin API key."),
    name = Some("x-admin-api-key"),
    in = Some("header"),
    scheme = None,
    bearerFormat = None,
    flows = None,
    openIdConnectUrl = None
  )

  private val jwtSecurityScheme = SecurityScheme(
    `type` = "http",
    description =
      Some("JWT Authentication. The header `Authorization` must be set with the JWT token using `Bearer` scheme"),
    name = Some("Authorization"),
    in = Some("header"),
    scheme = Some(AuthenticationScheme.Bearer.name),
    bearerFormat = None,
    flows = None,
    openIdConnectUrl = None
  )

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
