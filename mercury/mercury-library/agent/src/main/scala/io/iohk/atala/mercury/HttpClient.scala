package io.iohk.atala.mercury

import zio._

case class HttpResponseBody(bodyAsString: String)

trait HttpClient {
  def get(url: String): Task[HttpResponseBody]
  def postDIDComm(url: String, data: String): Task[HttpResponseBody]
}
