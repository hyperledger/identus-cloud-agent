package io.iohk.atala.agent.server.http

import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.*
import io.iohk.atala.agent.openapi.api.{DIDApi, DIDAuthenticationApi, DIDOperationsApi, IssueCredentialsApi}
import zio.*

object HttpRoutes {

  def routes: URIO[DIDApi & DIDOperationsApi & DIDAuthenticationApi & IssueCredentialsApi, Route] =
    for {
      didApi <- ZIO.service[DIDApi]
      didOperationsApi <- ZIO.service[DIDOperationsApi]
      didAuthApi <- ZIO.service[DIDAuthenticationApi]
      issueCredentialApi <- ZIO.service[IssueCredentialsApi]
    } yield didApi.route ~ didOperationsApi.route ~ didAuthApi.route ~ issueCredentialApi.route ~ additionalRoute

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
