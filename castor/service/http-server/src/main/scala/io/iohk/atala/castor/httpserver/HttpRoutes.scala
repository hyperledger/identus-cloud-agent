package io.iohk.atala.castor.httpserver

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.*
import io.iohk.atala.castor.openapi.api.DIDAuthenticationApi
import zio.*

object HttpRoutes {

  def routes: URIO[DIDAuthenticationApi, Route] =
    for {
      didAuthApi <- ZIO.service[DIDAuthenticationApi]
    } yield didAuthApi.route

}
