package io.iohk.atala.pollux.server.http

import akka.http.scaladsl.model.{ContentType, ContentTypes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.*
import io.iohk.atala.pollux.openapi.api.{IssueCredentialsApi, PresentProofApi, RevocationRegistryApi, SchemaRegistryApi}
import zio.*

object HttpRoutes {

//  def routes: URIO[IssueCredentialsApi & PresentProofApi & RevocationRegistryApi & SchemaRegistryApi, Route] =
  def routes: URIO[IssueCredentialsApi, Route] =
    for {
      issueCredentialsApi <- ZIO.service[IssueCredentialsApi]
//      presentProofApi <- ZIO.service[PresentProofApi]
//      revocationRegistryApi <- ZIO.service[RevocationRegistryApi]
//      schemaRegistryApi <- ZIO.service[SchemaRegistryApi]
    } yield additionalRoute ~ issueCredentialsApi.route
      // ~ presentProofApi.route ~ revocationRegistryApi.route ~ schemaRegistryApi.route

  private def additionalRoute: Route = {
    path("api" / "openapi-spec.yaml") {
      get {
        getFromResource("pollux-openapi-spec.yaml", ContentTypes.`text/plain(UTF-8)`)
      }
    }
  }

}
