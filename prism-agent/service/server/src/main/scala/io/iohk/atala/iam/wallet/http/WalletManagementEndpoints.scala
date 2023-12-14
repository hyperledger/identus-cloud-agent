package io.iohk.atala.iam.wallet.http

import io.iohk.atala.api.http.EndpointOutputs
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.iam.authentication.admin.AdminApiKeyCredentials
import io.iohk.atala.iam.authentication.admin.AdminApiKeySecurityLogic.adminApiKeyHeader
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.iam.authentication.oidc.JwtSecurityLogic.jwtAuthHeader
import io.iohk.atala.iam.wallet.http.model.CreateWalletRequest
import io.iohk.atala.iam.wallet.http.model.CreateWalletUmaPermissionRequest
import io.iohk.atala.iam.wallet.http.model.WalletDetail
import io.iohk.atala.iam.wallet.http.model.WalletDetailPage
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

import java.util.UUID

object WalletManagementEndpoints {

  private val baseEndpoint = endpoint
    .tag("Wallet Management")
    .securityIn(adminApiKeyHeader)
    .securityIn(apiKeyHeader)
    .securityIn(jwtAuthHeader)
    .in("wallets")
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val listWallet: Endpoint[
    (AdminApiKeyCredentials, ApiKeyCredentials, JwtCredentials),
    (RequestContext, PaginationInput),
    ErrorResponse,
    WalletDetailPage,
    Any
  ] =
    baseEndpoint.get
      .in(paginationInput)
      .errorOut(EndpointOutputs.basicFailuresAndForbidden)
      .out(statusCode(StatusCode.Ok).description("Successfully list all the wallets"))
      .out(jsonBody[WalletDetailPage])
      .summary("List all wallets")

  val getWallet: Endpoint[
    (AdminApiKeyCredentials, ApiKeyCredentials, JwtCredentials),
    (RequestContext, UUID),
    ErrorResponse,
    WalletDetail,
    Any
  ] =
    baseEndpoint.get
      .in(path[UUID]("walletId"))
      .errorOut(EndpointOutputs.basicFailureAndNotFoundAndForbidden)
      .out(statusCode(StatusCode.Ok).description("Successfully get the wallet"))
      .out(jsonBody[WalletDetail])
      .summary("Get the wallet by ID")

  val createWallet: Endpoint[
    (AdminApiKeyCredentials, ApiKeyCredentials, JwtCredentials),
    (RequestContext, CreateWalletRequest),
    ErrorResponse,
    WalletDetail,
    Any
  ] = baseEndpoint.post
    .in(jsonBody[CreateWalletRequest])
    .out(
      statusCode(StatusCode.Created).description("A new wallet has been created")
    )
    .out(jsonBody[WalletDetail])
    .errorOut(EndpointOutputs.basicFailuresAndForbidden)
    .name("createWallet")
    .summary("Create a new wallet")
    .description(
      """Create a new wallet with optional to use provided seed.
        |The seed will be used for DID key derivation inside the wallet.""".stripMargin
    )

  val createWalletUmaPermmission: Endpoint[
    (AdminApiKeyCredentials, ApiKeyCredentials, JwtCredentials),
    (RequestContext, UUID, CreateWalletUmaPermissionRequest),
    ErrorResponse,
    Unit,
    Any
  ] =
    baseEndpoint.post
      .in(path[UUID]("walletId") / "uma-permissions")
      .in(jsonBody[CreateWalletUmaPermissionRequest])
      .out(
        statusCode(StatusCode.Ok)
          .description("UMA resource permission is created on an authorization server.")
      )
      .errorOut(EndpointOutputs.basicFailuresAndForbidden)
      .name("createWalletUmaPermission")
      .summary("Create a UMA resource permission on an authorization server for the wallet.")

  val deleteWalletUmaPermmission: Endpoint[
    (AdminApiKeyCredentials, ApiKeyCredentials, JwtCredentials),
    (RequestContext, UUID, UUID),
    ErrorResponse,
    Unit,
    Any
  ] =
    baseEndpoint.delete
      .in(path[UUID]("walletId") / "uma-permissions")
      .in(query[UUID]("subject"))
      .out(
        statusCode(StatusCode.Ok)
          .description("UMA resource permission is removed from an authorization server.")
      )
      .errorOut(EndpointOutputs.basicFailuresAndForbidden)
      .name("deleteWalletUmaPermission")
      .summary("Delete a UMA resource permission on an authorization server for the wallet.")

}
