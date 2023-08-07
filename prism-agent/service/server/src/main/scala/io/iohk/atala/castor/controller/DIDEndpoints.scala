package io.iohk.atala.castor.controller

import io.iohk.atala.api.http.codec.DIDCodec.emptyDidJsonLD
import io.iohk.atala.api.http.codec.DIDCodec.{didJsonLD, didResolutionJsonLD}
import io.iohk.atala.castor.controller.http.{DIDResolutionResult, DIDInput}
import sttp.model.StatusCode
import sttp.tapir.*

object DIDEndpoints {

  private def matchStatus(sc: StatusCode): PartialFunction[Any, Boolean] = { case result: DIDResolutionResult =>
    val maybeError = result.didResolutionMetadata.error
    val isDeactivated = result.didDocumentMetadata.deactivated.getOrElse(false)
    maybeError match {
      case None if !isDeactivated             => sc == StatusCode.Ok
      case None                               => sc == StatusCode.Gone
      case Some("invalidDid")                 => sc == StatusCode.BadRequest
      case Some("invalidDidUrl")              => sc == StatusCode.BadRequest
      case Some("notFound")                   => sc == StatusCode.NotFound
      case Some("representationNotSupported") => sc == StatusCode.NotAcceptable
      case Some("internalError")              => sc == StatusCode.InternalServerError
      case Some(_)                            => false
    }
  }

  val resolutionEndpointOutput = oneOf[DIDResolutionResult](
    oneOfVariantValueMatcher(
      StatusCode.Ok,
      oneOfBody[DIDResolutionResult](
        stringBodyUtf8AnyFormat(didResolutionJsonLD),
        stringBodyUtf8AnyFormat(didJsonLD)
      )
    )(matchStatus(StatusCode.Ok)),
    oneOfVariantValueMatcher(
      StatusCode.BadRequest,
      oneOfBody[DIDResolutionResult](
        stringBodyUtf8AnyFormat(didResolutionJsonLD),
        stringBodyUtf8AnyFormat(emptyDidJsonLD)
      )
    )(matchStatus(StatusCode.BadRequest)),
    oneOfVariantValueMatcher(
      StatusCode.NotFound,
      oneOfBody[DIDResolutionResult](
        stringBodyUtf8AnyFormat(didResolutionJsonLD),
        stringBodyUtf8AnyFormat(emptyDidJsonLD)
      )
    )(matchStatus(StatusCode.NotFound)),
    oneOfVariantValueMatcher(
      StatusCode.NotAcceptable,
      oneOfBody[DIDResolutionResult](
        stringBodyUtf8AnyFormat(didResolutionJsonLD),
        stringBodyUtf8AnyFormat(emptyDidJsonLD)
      )
    )(matchStatus(StatusCode.NotAcceptable)),
    oneOfVariantValueMatcher(
      StatusCode.Gone,
      oneOfBody[DIDResolutionResult](
        stringBodyUtf8AnyFormat(didResolutionJsonLD),
        stringBodyUtf8AnyFormat(emptyDidJsonLD)
      )
    )(matchStatus(StatusCode.Gone)),
    oneOfVariantValueMatcher(
      StatusCode.NotImplemented,
      oneOfBody[DIDResolutionResult](
        stringBodyUtf8AnyFormat(didResolutionJsonLD),
        stringBodyUtf8AnyFormat(emptyDidJsonLD)
      )
    )(matchStatus(StatusCode.NotImplemented)),
    oneOfVariantValueMatcher(
      StatusCode.InternalServerError,
      oneOfBody[DIDResolutionResult](
        stringBodyUtf8AnyFormat(didResolutionJsonLD),
        stringBodyUtf8AnyFormat(emptyDidJsonLD)
      )
    )(_ => true),
  )

  // MUST conform to https://w3c-ccg.github.io/did-resolution/#bindings-https
  val getDID: PublicEndpoint[
    String,
    Nothing,
    DIDResolutionResult,
    Any
  ] = infallibleEndpoint.get
    .in("dids" / DIDInput.didRefPathSegment)
    .out(resolutionEndpointOutput)
    .name("getDID")
    .summary("Resolve Prism DID to a W3C representation")
    .description("""Resolve Prism DID to a W3C DID document representation.
      |The response can be the [DID resolution result](https://w3c-ccg.github.io/did-resolution/#did-resolution-result)
      |or [DID document representation](https://www.w3.org/TR/did-core/#representations) depending on the `Accept` request header.
      |The response is implemented according to [resolver HTTP binding](https://w3c-ccg.github.io/did-resolution/#bindings-https) in the DID resolution spec.
      |""".stripMargin)
    .tag("DID")

}
