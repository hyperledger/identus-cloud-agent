package io.iohk.atala.agent.server.http

import zio._
import zio.http._
import zio.http.model._
import zio.http.service._
import io.iohk.atala.mercury._

object ZioHttpClient {
  val layer = ZLayer.succeed(new ZioHttpClient())
}

class ZioHttpClient extends HttpClient {

  override def get(url: String): Task[HttpResponseBody] =
    zio.http.Client
      .request(url)
      .provideSomeLayer(zio.http.Client.default)
      .provideSomeLayer(zio.Scope.default)
      .flatMap(_.body.asString)
      .map(e => HttpResponseBody(e))

  def postDIDComm(url: String, data: String): Task[HttpResponseBody] =
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
      .flatMap(_.body.asString)
      .map(e => HttpResponseBody(e))
}
