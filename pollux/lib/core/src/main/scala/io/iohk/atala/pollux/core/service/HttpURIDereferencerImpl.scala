package io.iohk.atala.pollux.core.service
import io.iohk.atala.pollux.core.service.URIDereferencerError.{ConnectionError, ResourceNotFound, UnexpectedError}
import zio.http.*
import zio.http.ZClient.ClientLive
import zio.http.model.*
import zio.{IO, Layer, Scope, ULayer, URLayer, ZIO, ZLayer}

import java.net.URI

class HttpURIDereferencerImpl(client: Client) extends URIDereferencer {

  override def dereference(uri: URI): IO[URIDereferencerError, String] = {
    val result: ZIO[Client, URIDereferencerError, String] = for {
      response <- Client.request(uri.toString).mapError(t => ConnectionError(t.getMessage))
      body <- response match
        case Response(Status.Ok, _, body, _, None) =>
          body.asString.mapError(t => UnexpectedError(t.getMessage))
        case Response(Status.NotFound, _, _, _, None) =>
          ZIO.fail(ResourceNotFound(uri))
        case Response(_, _, _, _, httpError) =>
          ZIO.fail(UnexpectedError(s"HTTP response error: $httpError"))
    } yield body
    result.provide(ZLayer.succeed(client))
  }

}

object HttpURIDereferencerImpl {
  val layer: URLayer[Client, URIDereferencer] = ZLayer.fromFunction(HttpURIDereferencerImpl(_))
}
