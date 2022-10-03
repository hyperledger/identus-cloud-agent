package io.iohk.atala.agent.server.http

import akka.http.scaladsl.model.{ContentType, ContentTypes}
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
    } yield additionalRoute ~ didApi.route ~ didOperationsApi.route ~ didAuthApi.route ~ issueCredentialApi.route

  private def additionalRoute: Route = {
    path("api" / "openapi-spec.yaml") {
      get {
        getFromResource("prism-agent-openapi-spec.yaml", ContentTypes.`text/plain(UTF-8)`)
      }
    }
  }

}
