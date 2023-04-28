package io.iohk.atala.castor.controller

import io.iohk.atala.api.http.{EndpointOutputs, ErrorResponse, RequestContext}
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.castor.controller.http.{DIDInput, ManagedDIDPage}
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import io.iohk.atala.castor.controller.http.CreateManagedDidRequest
import io.iohk.atala.castor.controller.http.CreateManagedDIDResponse

object DIDRegistrarEndpoints {

  private val baseEndpoint = endpoint
    .tag("DID Registrar")
    .in("did-registrar")
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val createManagedDid: PublicEndpoint[
    (RequestContext, CreateManagedDidRequest),
    ErrorResponse,
    CreateManagedDIDResponse,
    Any
  ] = baseEndpoint.post
    .in("dids")
    .in(jsonBody[CreateManagedDidRequest])
    .errorOut(EndpointOutputs.basicFailures)
    .out(jsonBody[CreateManagedDIDResponse])

  val listManagedDid: PublicEndpoint[
    (RequestContext, PaginationInput),
    ErrorResponse,
    ManagedDIDPage,
    Any
  ] = baseEndpoint.get
    .in("dids")
    .in(paginationInput)
    .errorOut(EndpointOutputs.basicFailures)
    .out(jsonBody[ManagedDIDPage])

}
