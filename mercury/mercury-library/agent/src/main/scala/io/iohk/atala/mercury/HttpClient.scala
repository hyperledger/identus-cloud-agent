package io.iohk.atala.mercury

import zio._

case class HttpResponseBody(bodyAsString: String)

trait HttpClient {
  def get(url: String): Task[HttpResponseBody]
  def postDIDComm(url: String, data: String): Task[HttpResponseBody]
}

object HttpClient {
  def get(url: String): RIO[HttpClient, HttpResponseBody] =
    ZIO.serviceWithZIO(_.get(url))
  def postDIDComm(url: String, data: String): RIO[HttpClient, HttpResponseBody] =
    ZIO.serviceWithZIO(_.postDIDComm(url, data))
}
