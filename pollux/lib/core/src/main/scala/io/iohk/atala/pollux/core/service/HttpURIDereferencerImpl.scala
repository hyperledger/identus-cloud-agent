package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.service.URIDereferencerError.{ConnectionError, ResourceNotFound, UnexpectedError}
import zio.*
import zio.http.*
import zio.stream.ZSink

import java.net.URI
import java.nio.charset.StandardCharsets

class HttpURIDereferencerImpl(client: Client) extends URIDereferencer {

  override def dereference(uri: URI): IO[URIDereferencerError, String] = {
    for {
      url <- ZIO.fromOption(URL.fromURI(uri)).mapError(_ => ConnectionError(s"Invalid URI: $uri"))
      response <- client
        .request(Request(url = url))
        .mapError(t => ConnectionError(t.getMessage))
        .provideSomeLayer(zio.Scope.default)
      body <- response.status match {
        case Status.Ok =>
          response.body.asString.mapError(t => UnexpectedError(t.getMessage))
        case Status.NotFound =>
          ZIO.fail(ResourceNotFound(uri))
        case status if status.isError =>
          response.body.asStream
            .take(1024) // Only take the first 1024 bytes from the response body (if any).
            .runFold(Array.empty[Byte])(_ :+ _)
            .map(new String(_, StandardCharsets.UTF_8))
            .orDie
            .flatMap(errorMessage => ZIO.fail(UnexpectedError(s"HTTP response error: $status - $errorMessage")))
        case status =>
          ZIO.fail(UnexpectedError(s"Unexpected response status: $status"))
      }
    } yield body
  }

}

object HttpURIDereferencerImpl {
  val layer: URLayer[Client, URIDereferencer] = ZLayer.fromFunction(HttpURIDereferencerImpl(_))
}
