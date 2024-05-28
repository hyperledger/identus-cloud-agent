package org.hyperledger.identus.didcomm.controller

import org.hyperledger.identus.api.http.EndpointOutputs.basicFailuresAndNotFound
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import sttp.tapir.PublicEndpoint
import sttp.tapir.{endpoint, *}

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
    .errorOut(basicFailuresAndNotFound)
}
