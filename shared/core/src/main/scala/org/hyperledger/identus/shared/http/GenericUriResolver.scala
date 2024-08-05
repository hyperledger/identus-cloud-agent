package org.hyperledger.identus.shared.http

import io.lemonlabs.uri.{Uri, Url, Urn}
import org.hyperledger.identus.shared.models.{Failure, StatusCode}
import zio.*

import scala.util

trait UriResolver {

  def resolve(uri: String): IO[GenericUriResolverError, String]

}

class GenericUriResolver(resolvers: Map[String, UriResolver]) extends UriResolver {

  override def resolve(uri: String): IO[GenericUriResolverError, String] = {
    val parsedUri = Uri.parseTry(uri)

    ZIO.fromTry(parsedUri).mapError(_ => InvalidUri(uri)).flatMap {
      case url: Url =>
        url.schemeOption.fold(ZIO.fail(InvalidUri(uri)))(schema =>
          resolvers.get(schema).fold(ZIO.fail(UnsupportedUriSchema(schema))) { resolver =>
            resolver.resolve(uri)
          }
        )

      case Urn(path) => ZIO.fail(InvalidUri(uri)) // Must be a URL
    }

  }

}

trait GenericUriResolverError(val statusCode: StatusCode, val userFacingMessage: String) extends Failure {
  override val namespace: String = "UriResolver"
  def toThrowable: Throwable = {
    this match
      case InvalidUri(uri)              => new RuntimeException(s"Invalid URI: $uri")
      case UnsupportedUriSchema(schema) => new RuntimeException(s"Unsupported URI schema: $schema")
  }
}

case class InvalidUri(uri: String)
    extends GenericUriResolverError(StatusCode.UnprocessableContent, s"The URI to dereference is invalid: uri=[$uri]")

case class UnsupportedUriSchema(schema: String)
    extends GenericUriResolverError(StatusCode.UnprocessableContent, s"Unsupported URI schema: $schema")
