package io.iohk.atala.mercury

import zio._
import zio.http.{Header as _, *}
import io.iohk.atala.mercury._
object ZioHttpClient {
  val layer = ZLayer.succeed(new ZioHttpClient())
}

class ZioHttpClient extends HttpClient {

  override def get(url: String): Task[HttpResponse] =
    zio.http.Client
      .request(url)
      .provideSomeLayer(zio.http.Client.default)
      .provideSomeLayer(zio.Scope.default)
      .flatMap { response =>
        response.headers.toSeq.map(e => e)
        response.body.asString
          .map(body =>
            HttpResponse(
              response.status.code,
              response.headers.map(h => Header(h.headerName, h.renderedValue)).toSeq,
              body
            )
          )
      }

  def postDIDComm(url: String, data: String): Task[HttpResponse] =
    zio.http.Client
      .request(
        url = url, // TODO make ERROR type
        method = Method.POST,
        headers = Headers("content-type" -> "application/didcomm-encrypted+json"),
        // headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
        content = Body.fromChunk(Chunk.fromArray(data.getBytes)),
        // ssl = ClientSSLOptions.DefaultSSL,
      )
      .provideSomeLayer(zio.http.Client.default)
      .provideSomeLayer(zio.Scope.default)
      .flatMap { response =>
        response.headers.toSeq.map(e => e)
        response.body.asString
          .map(body =>
            HttpResponse(
              response.status.code,
              response.headers.map(h => Header(h.headerName, h.renderedValue)).toSeq,
              body
            )
          )
      }
}
