package io.iohk.atala.agent.server.http

import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.server.Directives.*
import io.iohk.atala.agent.openapi.api.{
  DIDApi,
  DIDAuthenticationApi,
  DIDOperationsApi,
  DIDRegistrarApi,
  IssueCredentialsApi
}
import zio.*
import io.iohk.atala.agent.openapi.api.IssueCredentialsProtocolApi
import akka.http.scaladsl.server.Route

object HttpRoutes {

  def routes: URIO[
    DIDApi & DIDOperationsApi & DIDAuthenticationApi & DIDRegistrarApi & IssueCredentialsApi & IssueCredentialsProtocolApi,
    Route
  ] =
    for {
      didApi <- ZIO.service[DIDApi]
      didOperationsApi <- ZIO.service[DIDOperationsApi]
      didAuthApi <- ZIO.service[DIDAuthenticationApi]
      disRegistrarApi <- ZIO.service[DIDRegistrarApi]
      issueCredentialApi <- ZIO.service[IssueCredentialsApi]
      issueCredentialsProtocolApi <- ZIO.service[IssueCredentialsProtocolApi]
    } yield didApi.route ~ didOperationsApi.route ~ didAuthApi.route ~ disRegistrarApi.route ~ issueCredentialApi.route ~ issueCredentialsProtocolApi.route ~ additionalRoute

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
