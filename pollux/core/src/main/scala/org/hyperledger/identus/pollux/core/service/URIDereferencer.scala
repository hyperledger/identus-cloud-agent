package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.shared.models.{Failure, StatusCode}
import zio.IO

import java.net.URI

trait URIDereferencer {
  def dereference(uri: URI): IO[URIDereferencerError, String]
}

sealed trait URIDereferencerError(
    val statusCode: StatusCode,
    val userFacingMessage: String
) extends Failure {
  override val namespace: String = "URIDereferencer"
}

object URIDereferencerError {
  final case class InvalidURI(uri: URI)
      extends URIDereferencerError(
        StatusCode.UnprocessableContent,
        s"The URI to dereference is invalid: uri=[$uri]"
      )

  final case class ConnectionError(cause: String)
      extends URIDereferencerError(
        StatusCode.BadGateway,
        s"An error occurred while connecting to the URI's underlying server: cause=[$cause]"
      )

  final case class ResourceNotFound(uri: URI)
      extends URIDereferencerError(
        StatusCode.NotFound,
        s"The resource was not found on the URI's underlying server: uri=[$uri]"
      )

  final case class ResponseProcessingError(cause: String)
      extends URIDereferencerError(
        StatusCode.BadGateway,
        s"An error occurred while processing the URI's underlying server response: cause=[$cause]"
      )

  final case class UnexpectedUpstreamResponseReceived(status: Int, content: Option[String] = None)
      extends URIDereferencerError(
        StatusCode.BadGateway,
        s"An unexpected response was received from the URI's underlying server: status=[$status], content=[${content.getOrElse("n/a")}]"
      )
}
