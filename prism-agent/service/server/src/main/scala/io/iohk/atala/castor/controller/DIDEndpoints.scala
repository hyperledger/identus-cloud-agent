package io.iohk.atala.castor.controller

import io.iohk.atala.api.http.codec.DIDCodec.{didJsonLD, didResolutionJsonLD}
import io.iohk.atala.castor.controller.http.{DIDResolutionResult, DIDInput}
import sttp.model.StatusCode
import sttp.tapir.*
import io.iohk.atala.api.http.codec.DIDCodec.emptyDidJsonLD
import sttp.tapir.typelevel.MatchType

object DIDEndpoints {

  private def matchStatus(sc: StatusCode): PartialFunction[Any, Boolean] = {
    case result: DIDResolutionResult =>
      val maybeError = result.didResolutionMetadata.error
      val isDeactivated = result.didDocumentMetadata.deactivated.getOrElse(false)
      maybeError match {
        case None if !isDeactivated => sc == StatusCode.Ok
        case None => sc == StatusCode.Gone
        case Some("invalidDid") => sc == StatusCode.BadRequest
        case Some("notFound") => sc == StatusCode.NotFound
        case Some(_) => false
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
    // .out(
    //   statusCode
    //     .description(StatusCode.Ok, "The resolution result or W3C DID document representation")
    //     .description(StatusCode.BadRequest, "Invalid DID or DID URL")
    //     .description(StatusCode.NotFound, "The DID is not found")
    //     .description(StatusCode.NotAcceptable, "The DID document representation is not supported")
    //     .description(StatusCode.Gone, "The DID is deactivated")
    //     .description(StatusCode.NotImplemented, "The DID method is not supported")
    //     .description(StatusCode.InternalServerError, "Internal error")
    //     .and(
    //       oneOf[DIDResolutionResult](
    //         oneOfVariant(stringBodyUtf8AnyFormat(didResolutionJsonLD)),
    //         oneOfVariant(stringBodyUtf8AnyFormat(didJsonLD)),
    //       )
    //     )
    // )
    .name("getDID")
    .summary("Resolve Prism DID to a W3C representation")
    .description("""Resolve Prism DID to a W3C DID document representation.
      |The response can be the [DID resolution result](https://w3c-ccg.github.io/did-resolution/#did-resolution-result)
      |or [DID document representation](https://www.w3.org/TR/did-core/#representations) depending on the `Accept` request header.
      |The response is implemented according to [resolver HTTP binding](https://w3c-ccg.github.io/did-resolution/#bindings-https) in the DID resolution spec.
      |""".stripMargin)
    .tag("DID")

}
