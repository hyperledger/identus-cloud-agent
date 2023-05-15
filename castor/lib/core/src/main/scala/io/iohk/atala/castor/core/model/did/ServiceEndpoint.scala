package io.iohk.atala.castor.core.model.did

import io.circe.JsonObject
import io.iohk.atala.castor.core.util.UriUtils

/** Marker for type that can be used in sevice endpoint list
  */
sealed trait UriOrJsonEndpoint

sealed trait ServiceEndpoint {
  def normalize(): ServiceEndpoint
}

object ServiceEndpoint {

  final case class URI private[did] (uri: String) extends ServiceEndpoint, UriOrJsonEndpoint {
    override def normalize(): URI = {
      UriUtils.normalizeUri(uri).map(URI(_)).get // uri is already validated
    }
  }

  object URI {
    def fromString(s: String): Either[String, ServiceEndpoint.URI] = {
      UriUtils
        .normalizeUri(s)
        .toRight(s"unable to parse service endpoint URI: \"$s\"")
        .map(_ => ServiceEndpoint.URI(s))
    }
  }

  final case class Json(json: JsonObject) extends ServiceEndpoint, UriOrJsonEndpoint {
    override def normalize(): Json = this
  }

  final case class EndpointList(head: UriOrJsonEndpoint, tail: Seq[UriOrJsonEndpoint]) extends ServiceEndpoint {
    def values: Seq[UriOrJsonEndpoint] = head +: tail

    override def normalize(): EndpointList = {
      EndpointList(
        head = normalizeUriOrJsonEndpoint(head),
        tail = tail.map(normalizeUriOrJsonEndpoint)
      )
    }

    private def normalizeUriOrJsonEndpoint(endpoint: UriOrJsonEndpoint): UriOrJsonEndpoint = {
      endpoint match {
        case uri: URI   => uri.normalize()
        case json: Json => json.normalize()
      }
    }
  }

}
