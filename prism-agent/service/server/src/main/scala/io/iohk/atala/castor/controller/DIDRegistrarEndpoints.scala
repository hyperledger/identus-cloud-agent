package io.iohk.atala.castor.controller

import io.iohk.atala.api.http.{EndpointOutputs, ErrorResponse, RequestContext}
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.castor.controller.http.{DIDInput, ManagedDIDPage}
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

object DIDRegistrarEndpoints {

  private val baseEndpoint = endpoint
    .tag("DID Registrar") // TODO: change this before PR
    .in("did-registrar")
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val listManagedDid: PublicEndpoint[
    (RequestContext, String, PaginationInput),
    ErrorResponse,
    ManagedDIDPage,
    Any
  ] = baseEndpoint.get
    .in("dids" / DIDInput.didRefPathSegment)
    .in(paginationInput)
    .errorOut(EndpointOutputs.basicFailures)
    .out(jsonBody[ManagedDIDPage])

}
