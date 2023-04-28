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
import scala.tools.nsc.doc.model.Public
import io.iohk.atala.castor.controller.http.DIDOperationResponse
import sttp.model.StatusCode

object DIDRegistrarEndpoints {

  private val baseEndpoint = endpoint
    .tag("DID Registrar")
    .in("did-registrar" / "did")
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val listManagedDid: PublicEndpoint[
    (RequestContext, PaginationInput),
    ErrorResponse,
    ManagedDIDPage,
    Any
  ] = baseEndpoint.get
    .in(paginationInput)
    .errorOut(EndpointOutputs.basicFailures)
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[ManagedDIDPage])

  val createManagedDid: PublicEndpoint[
    (RequestContext, CreateManagedDidRequest),
    ErrorResponse,
    CreateManagedDIDResponse,
    Any
  ] = baseEndpoint.post
    .in(jsonBody[CreateManagedDidRequest])
    .errorOut(EndpointOutputs.basicFailures)
    .out(statusCode(StatusCode.Created))
    .out(jsonBody[CreateManagedDIDResponse])

  val getManagedDid: PublicEndpoint[
    (RequestContext, String),
    ErrorResponse,
    ManagedDID,
    Any
  ] = baseEndpoint.get
    .in(DIDInput.didRefPathSegment)
    .errorOut(EndpointOutputs.basicFailuresAndNotFound)
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[ManagedDID])

  val publishManagedDid: PublicEndpoint[
    (RequestContext, String),
    ErrorResponse,
    DIDOperationResponse,
    Any
  ] = baseEndpoint.post
    .in(DIDInput.didRefPathSegment / "publish")
    .errorOut(EndpointOutputs.basicFailuresAndNotFound)
    .out(statusCode(StatusCode.Accepted))
    .out(jsonBody[DIDOperationResponse])

}
