package io.iohk.atala.agent.server.http

import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import io.iohk.atala.agent.openapi.api.PresentProofApi
import zio.*

object HttpRoutes {

  def routes: URIO[
    PresentProofApi,
    Route
  ] =
    for {
      presentProofApi <- ZIO.service[PresentProofApi]
    } yield presentProofApi.route ~
      additionalRoute

  private def additionalRoute: Route = {
    // swagger-ui expects this particular header when resolving relative $ref
    val yamlContentType = ContentType.parse("application/yaml").toOption.get

    pathPrefix("api") {
      path("openapi-spec.yaml") {
        getFromResource("http/prism-agent-openapi-spec.yaml", yamlContentType)
      } ~
        getFromResourceDirectory("http")(_ => yamlContentType)
    }
  }

}
