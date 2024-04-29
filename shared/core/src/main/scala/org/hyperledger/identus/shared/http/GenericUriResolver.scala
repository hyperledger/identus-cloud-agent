package org.hyperledger.identus.shared.http

import zio.*
import io.lemonlabs.uri.{DataUrl, Uri, Url, Urn}

trait UriResolver {

  def resolve(uri: String): IO[GenericUriResolverError, String]

}

class GenericUriResolver(resolvers: Map[String, UriResolver]) extends UriResolver {

  override def resolve(uri: String): IO[GenericUriResolverError, String] = {
    val parsedUri = Uri.parse(uri)
    parsedUri match
      case url: Url =>
        url.schemeOption.fold(ZIO.fail(InvalidUri(uri)))(schema =>
          resolvers.get(schema).fold(ZIO.fail(UnsupportedUriSchema(schema)))(resolver => resolver.resolve(uri))
        )

      case Urn(path) => ZIO.fail(InvalidUri(uri)) // Must be a URL
  }

}

class DataUrlResolver extends UriResolver {
  override def resolve(dataUrl: String): IO[GenericUriResolverError, String] = {

    DataUrl.parseOption(dataUrl).fold(ZIO.fail(InvalidUri(dataUrl))) { url =>
      ZIO.succeed(String(url.data, url.mediaType.charset))
    }

  }

}

sealed trait GenericUriResolverError {
  def toThrowable: Throwable = {
    this match
      case InvalidUri(uri)              => new RuntimeException(s"Invalid URI: $uri")
      case UnsupportedUriSchema(schema) => new RuntimeException(s"Unsupported URI schema: $schema")
      case SchemaSpecificResolutionError(schema, error) =>
        new RuntimeException(s"Error resolving ${schema} URL: ${error.getMessage}")
  }
}

case class InvalidUri(uri: String) extends GenericUriResolverError

case class UnsupportedUriSchema(schema: String) extends GenericUriResolverError

case class SchemaSpecificResolutionError(schema: String, error: Throwable) extends GenericUriResolverError
