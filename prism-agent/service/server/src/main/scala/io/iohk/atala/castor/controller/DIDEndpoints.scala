package io.iohk.atala.castor.controller

import io.iohk.atala.api.http.codec.CustomMediaTypes
import io.iohk.atala.api.http.codec.DIDCodec.{didJsonLD, didResolutionJsonLD}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import sttp.model.{Header, StatusCode}
import sttp.tapir.*
import io.iohk.atala.castor.controller.http.{DIDResolutionResult, DIDInput}
import sttp.tapir.json.zio.jsonBody

object DIDEndpoints {

  // MUST conform to https://w3c-ccg.github.io/did-resolution/#bindings-https
  val getDID: PublicEndpoint[
    String,
    Nothing,
    (StatusCode, DIDResolutionResult),
    Any
  ] = infallibleEndpoint.get
    .in("dids" / DIDInput.didRefPathSegment)
    .out(
      statusCode
        .description(StatusCode.Ok, "The resolution result or W3C DID document representation")
        .description(StatusCode.BadRequest, "Invalid DID or DID URL")
        .description(StatusCode.NotFound, "The DID is not found")
        .description(StatusCode.NotAcceptable, "The DID document representation is not supported")
        .description(StatusCode.Gone, "The DID is deactivated")
        .description(StatusCode.NotImplemented, "The DID method is not supported")
        .description(StatusCode.InternalServerError, "Internal error")
        .and(
          oneOf[DIDResolutionResult](
            oneOfVariant(stringBodyUtf8AnyFormat(didResolutionJsonLD)),
            oneOfVariant(stringBodyUtf8AnyFormat(didJsonLD)),
          )
        )
    )
    .name("getDID")
    .summary("Resolve Prism DID to a W3C representation")
    .description("""Resolve Prism DID to a W3C DID document representation.
      |The response can be the [DID resolution result](https://w3c-ccg.github.io/did-resolution/#did-resolution-result)
      |or [DID document representation](https://www.w3.org/TR/did-core/#representations) depending on the `Accept` request header.
      |The response is implemented according to [resolver HTTP binding](https://w3c-ccg.github.io/did-resolution/#bindings-https) in the DID resolution spec.
      |""".stripMargin)
    .tag("DID")

}
