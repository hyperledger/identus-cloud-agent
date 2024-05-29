package org.hyperledger.identus.didcomm.controller

import org.hyperledger.identus.api.http.EndpointOutputs.{FailureVariant, basicFailuresWith}
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.didcomm.controller.http.DIDCommMessage
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.{PublicEndpoint, endpoint, *}

object DIDCommEndpoints {
  val handleDIDCommMessage: PublicEndpoint[
    (RequestContext, DIDCommMessage),
    ErrorResponse,
    Unit,
    Any
  ] = endpoint.post
    .in(extractFromRequest[RequestContext](RequestContext.apply))
    .in(jsonBody[DIDCommMessage])
    .in("")
    .out(emptyOutput)
    .errorOut(basicFailuresWith(FailureVariant.unprocessableEntity, FailureVariant.notFound))
}
