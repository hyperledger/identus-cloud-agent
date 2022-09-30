package io.iohk.atala.agent.server.http

import akka.http.scaladsl.model.{ContentType, ContentTypes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.*
import io.iohk.atala.agent.openapi.api.{DIDApi, DIDAuthenticationApi, DIDOperationsApi}
import zio.*

object HttpRoutes {

  def routes: URIO[DIDApi & DIDOperationsApi & DIDAuthenticationApi, Route] =
    for {
      didApi <- ZIO.service[DIDApi]
      didOperationsApi <- ZIO.service[DIDOperationsApi]
      didAuthApi <- ZIO.service[DIDAuthenticationApi]
    } yield additionalRoute ~ didApi.route ~ didOperationsApi.route ~ didAuthApi.route

  private def additionalRoute: Route = {
    path("api" / "openapi-spec.yaml") {
      get {
        getFromResource("castor-openapi-spec.yaml", ContentTypes.`text/plain(UTF-8)`)
      }
    }
  }

}
