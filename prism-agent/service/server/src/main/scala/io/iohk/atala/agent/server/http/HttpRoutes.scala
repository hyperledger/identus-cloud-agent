package io.iohk.atala.agent.server.http

import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.server.Directives.*
import io.iohk.atala.agent.openapi.api.{
  PresentProofApi,
  DIDApi,
  DIDAuthenticationApi,
  DIDOperationsApi,
  DIDRegistrarApi,
  IssueCredentialsProtocolApi,
  ConnectionsManagementApi
}
import zio.*
import akka.http.scaladsl.server.Route

object HttpRoutes {

  def routes: URIO[
    DIDApi & DIDOperationsApi & DIDAuthenticationApi & DIDRegistrarApi & IssueCredentialsProtocolApi &
      ConnectionsManagementApi & PresentProofApi,
    Route
  ] =
    for {
      didApi <- ZIO.service[DIDApi]
      didOperationsApi <- ZIO.service[DIDOperationsApi]
      didAuthApi <- ZIO.service[DIDAuthenticationApi]
      disRegistrarApi <- ZIO.service[DIDRegistrarApi]
      issueCredentialsProtocolApi <- ZIO.service[IssueCredentialsProtocolApi]
      connectionsManagementApi <- ZIO.service[ConnectionsManagementApi]
      presentProofApi <- ZIO.service[PresentProofApi]
    } yield didApi.route ~
      didOperationsApi.route ~
      didAuthApi.route ~
      disRegistrarApi.route ~
      issueCredentialsProtocolApi.route ~
      connectionsManagementApi.route ~
      presentProofApi.route ~
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
