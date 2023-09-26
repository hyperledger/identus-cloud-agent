package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.service.URIDereferencerError.{ConnectionError, ResourceNotFound, UnexpectedError}
import zio.http.*
import zio.*

import java.net.URI

class HttpURIDereferencerImpl(client: Client) extends URIDereferencer {

  override def dereference(uri: URI): IO[URIDereferencerError, String] = {
    val result: ZIO[Client, URIDereferencerError, String] = for {
      response <- Client.request(uri.toString).mapError(t => ConnectionError(t.getMessage))
      body <- response.status match {
        case Status.Ok =>
          response.body.asString.mapError(t => UnexpectedError(t.getMessage))
        case Status.NotFound if !response.status.isError => ZIO.fail(ResourceNotFound(uri))
        case _ if response.status.isError =>
          val err = response match {
            case Response.GetError(error) => Some(error)
            case _                        => None
          }
          ZIO.fail(UnexpectedError(s"HTTP response error: $err"))
        case _ =>
          ZIO.fail(UnexpectedError("Unknown error"))
      }
    } yield body
    result.provide(ZLayer.succeed(client))
  }

}

object HttpURIDereferencerImpl {
  val layer: URLayer[Client, URIDereferencer] = ZLayer.fromFunction(HttpURIDereferencerImpl(_))
}
