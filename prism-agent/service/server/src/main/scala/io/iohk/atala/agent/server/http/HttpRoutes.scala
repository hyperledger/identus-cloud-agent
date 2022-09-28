package io.iohk.atala.agent.server.http

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
    } yield didApi.route ~ didOperationsApi.route ~ didAuthApi.route

}
