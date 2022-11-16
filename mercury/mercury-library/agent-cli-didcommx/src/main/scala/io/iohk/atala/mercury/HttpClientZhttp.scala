package io.iohk.atala.mercury

import zio._
import zhttp.service._
import zhttp.http._
import io.iohk.atala.mercury._

object HttpClientZhttp {
  val layer = ZLayer.succeed(new HttpClientZhttp())
}

class HttpClientZhttp extends HttpClient {

  val env = ChannelFactory.auto ++ EventLoopGroup.auto()

  override def get(url: String): Task[HttpResponseBody] =
    Client
      .request(url)
      .provideSomeLayer(env)
      .flatMap(_.body.asString)
      .map(e => HttpResponseBody(e))

  def postDIDComm(url: String, data: String): Task[HttpResponseBody] =
    Client
      .request(
        url = url, // TODO make ERROR type
        method = Method.POST,
        headers = Headers("content-type" -> "application/didcomm-encrypted+json"),
        // headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
        content = Body.fromChunk(Chunk.fromArray(data.getBytes)),
        // ssl = ClientSSLOptions.DefaultSSL,
      )
      .provideSomeLayer(env)
      .flatMap(_.body.asString)
      .map(e => HttpResponseBody(e))
}
