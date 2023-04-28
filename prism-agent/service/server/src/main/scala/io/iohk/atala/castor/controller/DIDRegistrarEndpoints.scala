package io.iohk.atala.castor.controller

import io.iohk.atala.api.http.{EndpointOutputs, ErrorResponse, RequestContext}
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.castor.controller.http.{
  CreateManagedDIDResponse,
  CreateManagedDidRequest,
  DIDInput,
  ManagedDID,
  ManagedDIDPage
}
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

object DIDRegistrarEndpoints {

  private val baseEndpoint = endpoint
    .tag("DID Registrar")
    .in("did-registrar")
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

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

  val getManagedDid: PublicEndpoint[
    (RequestContext, String),
    ErrorResponse,
    ManagedDID,
    Any
  ] = baseEndpoint.get
    .in("dids" / DIDInput.didRefPathSegment)
    .errorOut(EndpointOutputs.basicFailuresAndNotFound)
    .out(jsonBody[ManagedDID])

}
