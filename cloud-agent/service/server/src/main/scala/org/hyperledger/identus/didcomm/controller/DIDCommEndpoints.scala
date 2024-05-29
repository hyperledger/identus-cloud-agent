package org.hyperledger.identus.didcomm.controller

import org.hyperledger.identus.api.http.EndpointOutputs.{FailureVariant, basicFailuresWith}
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import sttp.tapir.{PublicEndpoint, endpoint, *}

object DIDCommEndpoints {
  val handleDIDCommMessage: PublicEndpoint[
    (RequestContext, String),
    ErrorResponse,
    Unit,
    Any
  ] = endpoint.post
    .in(extractFromRequest[RequestContext](RequestContext.apply))
    .in(stringBody)
    .in("")
    .out(emptyOutput)
    .errorOut(basicFailuresWith(FailureVariant.unprocessableEntity, FailureVariant.notFound))
}
