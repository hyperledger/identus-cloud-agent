package org.hyperledger.identus.pollux.core.service.uriResolvers

import io.lemonlabs.uri.{Url, UrlPath}
import org.hyperledger.identus.pollux.vc.jwt
import org.hyperledger.identus.pollux.vc.jwt.*
import org.hyperledger.identus.shared.crypto.Sha256Hash
import org.hyperledger.identus.shared.http.{GenericUriResolverError, UriResolver}
import org.hyperledger.identus.shared.models.StatusCode
import zio.*

class DidUrlResolver(httpUrlResolver: HttpUrlResolver, didResolver: DidResolver) extends UriResolver {
  import DidUrlResolver.*

  def resolve(uri: String): IO[GenericUriResolverError, String] = {

    for {
      parsed <- ZIO.fromTry(Url.parseTry(uri)).mapError(_ => InvalidURI(uri))
      maybeResourceService = parsed.query.param("resourceService")
      maybeResourcePath = parsed.query.param("resourcePath")
      maybeResourceHash = parsed.query.param("resourceHash")
      serviceAndPath <- ZIO
        .fromOption(maybeResourceService zip maybeResourcePath)
        .mapError(_ => MissingRequiredParams(uri))
      (resourceService, resourcePath) = serviceAndPath
      didStr = parsed.removeQueryString().toString
      didCoument <- didResolver.resolve(didStr).flatMap {
        case DIDResolutionFailed(err) =>
          err match
            case InvalidDid(message)                 => ZIO.fail(DidResolutionError(didStr, message))
            case NotFound(message)                   => ZIO.fail(DidResolutionError(didStr, message))
            case RepresentationNotSupported(message) => ZIO.fail(DidResolutionError(didStr, message))
            case InvalidPublicKeyLength(message)     => ZIO.fail(DidResolutionError(didStr, message))
            case InvalidPublicKeyType(message)       => ZIO.fail(DidResolutionError(didStr, message))
            case UnsupportedPublicKeyType(message)   => ZIO.fail(DidResolutionError(didStr, message))
            case jwt.Error(error, message)           => ZIO.fail(DidResolutionError(didStr, message))
        case DIDResolutionSucceeded(didDocument, didDocumentMetadata) => ZIO.succeed(didDocument)
      }
      service <- ZIO
        .fromOption(didCoument.service.find(x => x.id == s"$didStr#$resourceService" && x.`type` == "LinkedResourceV1"))
        .mapError(_ =>
          DidDocumentParsingError(
            s"""Service with id: "$resourceService" and type: "LinkedResourceV1" not found inside DID document"""
          )
        )
      baseUrl <- ZIO
        .fromOption(service.serviceEndpoint.asString)
        .mapError(_ => DidDocumentParsingError("serviceEndpoint is not a string"))

      path <- ZIO.fromOption(UrlPath.parseOption(resourcePath)).mapError(_ => InvalidUrlPath(resourcePath))
      finalUrl <- ZIO
        .fromTry(Url.parseTry(baseUrl).map(x => x.withPath(path)).map(_.toString))
        .mapError(_ => InvalidURI(baseUrl))
      result <- httpUrlResolver.resolve(finalUrl)

      validatedResult <- maybeResourceHash.fold(ZIO.succeed(result)) { hash =>
        val computedHash = Sha256Hash.compute(result.getBytes()).hexEncoded
        if (computedHash == hash) ZIO.succeed(result)
        else ZIO.fail(InvalidHash(hash, computedHash))
      }

    } yield validatedResult

  }

}

object DidUrlResolver {

  class DidUrlResolverError(statusCode: StatusCode, userFacingMessage: String)
      extends GenericUriResolverError(statusCode, userFacingMessage)

  final case class InvalidURI(uri: String)
      extends DidUrlResolverError(
        StatusCode.UnprocessableContent,
        s"The URI to resolve is invalid: uri=[$uri]"
      )

  final case class InvalidUrlPath(path: String)
      extends DidUrlResolverError(
        StatusCode.UnprocessableContent,
        s"Invalid URL path: $path"
      )

  final case class MissingRequiredParams(url: String)
      extends DidUrlResolverError(
        StatusCode.UnprocessableContent,
        s"DID URL must have resourcePath and resourceService query parameters, got invalid URL: $url"
      )

  final case class DidResolutionError(didStr: String, reason: String)
      extends DidUrlResolverError(
        StatusCode.InternalServerError,
        s"Error resolving DID: $didStr, error: $reason"
      )

  final case class DidDocumentParsingError(customMessage: String)
      extends DidUrlResolverError(
        StatusCode.InternalServerError,
        s"Error parsing DID document: $customMessage"
      )

  final case class InvalidHash(expectedHash: String, computedHash: String)
      extends DidUrlResolverError(
        StatusCode.UnprocessableContent,
        s"Invalid hash, expected: $expectedHash, computed: $computedHash"
      )

  val layer: URLayer[HttpUrlResolver & DidResolver, DidUrlResolver] =
    ZLayer.fromFunction(DidUrlResolver(_, _))
}
