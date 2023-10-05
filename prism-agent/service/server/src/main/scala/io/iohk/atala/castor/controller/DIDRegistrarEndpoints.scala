package io.iohk.atala.castor.controller

import io.iohk.atala.api.http.EndpointOutputs.FailureVariant
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{EndpointOutputs, ErrorResponse, RequestContext}
import io.iohk.atala.castor.controller.http.DIDOperationResponse
import io.iohk.atala.castor.controller.http.UpdateManagedDIDRequest
import io.iohk.atala.castor.controller.http.{
  CreateManagedDIDResponse,
  CreateManagedDidRequest,
  DIDInput,
  ManagedDID,
  ManagedDIDPage
}
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.iam.authentication.oidc.JwtSecurityLogic.bearerAuthHeader
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

object DIDRegistrarEndpoints {

  private val baseEndpoint = endpoint
    .tag("DID Registrar")
    .in("did-registrar" / "dids")
    .in(extractFromRequest[RequestContext](RequestContext.apply))
    .securityIn(apiKeyHeader)

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val listManagedDid: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, PaginationInput),
    ErrorResponse,
    ManagedDIDPage,
    Any
  ] = baseEndpoint.get
    .securityIn(bearerAuthHeader)
    .in(paginationInput)
    .errorOut(EndpointOutputs.basicFailuresAndForbidden)
    .out(statusCode(StatusCode.Ok).description("List Prism Agent managed DIDs"))
    .out(jsonBody[ManagedDIDPage])
    .summary("List all DIDs stored in Prism Agent's wallet")
    .description(
      """List all DIDs stored in Prism Agent's wallet.
        |Return a paginated items ordered by created timestamp.
        |If the `limit` parameter is not set, it defaults to 100 items per page.""".stripMargin
    )

  val createManagedDid: Endpoint[
    ApiKeyCredentials,
    (RequestContext, CreateManagedDidRequest),
    ErrorResponse,
    CreateManagedDIDResponse,
    Any
  ] = baseEndpoint.post
    .in(jsonBody[CreateManagedDidRequest])
    .errorOut(
      EndpointOutputs
        .basicFailuresWith(FailureVariant.unprocessableEntity, FailureVariant.notFound, FailureVariant.forbidden)
    )
    .out(statusCode(StatusCode.Created).description("Created unpublished DID."))
    .out(jsonBody[CreateManagedDIDResponse])
    .summary("Create unpublished DID and store it in Prism Agent's wallet")
    .description(
      """Create unpublished DID and store it inside Prism Agent's wallet. The private keys of the DID is
        |managed by Prism Agent. The DID can later be published to the VDR using publications endpoint.""".stripMargin
    )

  val getManagedDid: Endpoint[
    ApiKeyCredentials,
    (RequestContext, String),
    ErrorResponse,
    ManagedDID,
    Any
  ] = baseEndpoint.get
    .in(DIDInput.didRefPathSegment)
    .errorOut(EndpointOutputs.basicFailureAndNotFoundAndForbidden)
    .out(statusCode(StatusCode.Ok).description("Get Prism Agent managed DID"))
    .out(jsonBody[ManagedDID])
    .summary("Get DID stored in Prism Agent's wallet")
    .description("Get DID stored in Prism Agent's wallet")

  val publishManagedDid: Endpoint[
    ApiKeyCredentials,
    (RequestContext, String),
    ErrorResponse,
    DIDOperationResponse,
    Any
  ] = baseEndpoint.post
    .in(DIDInput.didRefPathSegment / "publications")
    .errorOut(EndpointOutputs.basicFailureAndNotFoundAndForbidden)
    .out(statusCode(StatusCode.Accepted).description("Publishing DID to the VDR."))
    .out(jsonBody[DIDOperationResponse])
    .summary("Publish the DID stored in Prism Agent's wallet to the VDR")
    .description("Publish the DID stored in Prism Agent's wallet to the VDR.")

  val updateManagedDid: Endpoint[
    ApiKeyCredentials,
    (RequestContext, String, UpdateManagedDIDRequest),
    ErrorResponse,
    DIDOperationResponse,
    Any
  ] = baseEndpoint.post
    .in(DIDInput.didRefPathSegment / "updates")
    .in(jsonBody[UpdateManagedDIDRequest])
    .errorOut(
      EndpointOutputs
        .basicFailuresWith(
          FailureVariant.unprocessableEntity,
          FailureVariant.notFound,
          FailureVariant.conflict,
          FailureVariant.forbidden
        )
    )
    .out(statusCode(StatusCode.Accepted).description("DID update operation accepted"))
    .out(jsonBody[DIDOperationResponse])
    .summary("Update DID in Prism Agent's wallet and post update operation to the VDR")
    .description(
      """Update DID in Prism Agent's wallet and post update operation to the VDR.
        |This endpoint updates the DID document from the last confirmed operation.
        |Submitting multiple update operations without waiting for confirmation will result in
        |some operations being rejected as only one operation is allowed to be appended to the last confirmed operation.""".stripMargin
    )

  val deactivateManagedDid: Endpoint[
    ApiKeyCredentials,
    (RequestContext, String),
    ErrorResponse,
    DIDOperationResponse,
    Any
  ] = baseEndpoint.post
    .in(DIDInput.didRefPathSegment / "deactivations")
    .errorOut(
      EndpointOutputs
        .basicFailuresWith(FailureVariant.unprocessableEntity, FailureVariant.notFound, FailureVariant.forbidden)
    )
    .out(statusCode(StatusCode.Accepted).description("DID deactivation operation accepted"))
    .out(jsonBody[DIDOperationResponse])
    .summary("Deactivate DID in Prism Agent's wallet and post deactivate operation to the VDR")
    .description("Deactivate DID in Prism Agent's wallet and post deactivate operation to the VDR.")

}
