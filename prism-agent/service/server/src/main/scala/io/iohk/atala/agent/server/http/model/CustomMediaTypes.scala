package io.iohk.atala.agent.server.http.model

import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.HttpCharsets

object CustomMediaTypes {

  val `application/did+ld+json`: MediaType.WithFixedCharset =
    MediaType.customWithFixedCharset("application", "did+ld+json", HttpCharsets.`UTF-8`)

  val `application/ld+json;did-resolution`: MediaType.WithFixedCharset = MediaType.customWithFixedCharset(
    "application",
    "ld+json",
    HttpCharsets.`UTF-8`,
    params = Map("profile" -> "https://w3id.org/did-resolution")
  )

}
