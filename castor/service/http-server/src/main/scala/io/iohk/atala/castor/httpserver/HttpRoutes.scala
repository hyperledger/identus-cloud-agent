package io.iohk.atala.castor.httpserver

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.*
import io.iohk.atala.castor.openapi.api.{DIDApi, DIDAuthenticationApi}
import zio.*

object HttpRoutes {

  def routes: URIO[DIDApi & DIDAuthenticationApi, Route] =
    for {
      didApi <- ZIO.service[DIDApi]
      didAuthApi <- ZIO.service[DIDAuthenticationApi]
    } yield didApi.route ~ didAuthApi.route

}
