package io.iohk.atala.iam.wallet.http

import io.iohk.atala.api.http.EndpointOutputs
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.iam.authentication.admin.AdminApiKeyCredentials
import io.iohk.atala.iam.authentication.admin.AdminApiKeySecurityLogic.adminApiKeyHeader
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import io.iohk.atala.iam.wallet.http.model.CreateWalletRequest
import io.iohk.atala.iam.wallet.http.model.WalletDetail
import io.iohk.atala.iam.wallet.http.model.WalletDetailPage
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

import java.util.UUID

object WalletManagementEndpoints {

  private val baseWalletAdminEndpoint = endpoint
    .tag("Wallet Management")
    .securityIn(adminApiKeyHeader)
    .in("wallets")
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  private val baseWalletNotificationEndpoint = endpoint
    .tag("Wallet Notification Management")
    .securityIn(apiKeyHeader)
    .in("notifications")
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val listWallet: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, PaginationInput),
    ErrorResponse,
    WalletDetailPage,
    Any
  ] =
    baseWalletAdminEndpoint.get
      .in(paginationInput)
      .errorOut(EndpointOutputs.basicFailuresAndForbidden)
      .out(statusCode(StatusCode.Ok).description("List Prism Agent managed DIDs"))
      .out(jsonBody[WalletDetailPage])
      .summary("List all wallets")

  val getWallet: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, UUID),
    ErrorResponse,
    WalletDetail,
    Any
  ] =
    baseWalletAdminEndpoint.get
      .in(path[UUID]("walletId"))
      .errorOut(EndpointOutputs.basicFailureAndNotFoundAndForbidden)
      .out(statusCode(StatusCode.Ok).description("Successfully get the wallet"))
      .out(jsonBody[WalletDetail])
      .summary("Get the wallet by ID")

  val createWallet: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, CreateWalletRequest),
    ErrorResponse,
    WalletDetail,
    Any
  ] = baseWalletAdminEndpoint.post
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

  val createWalletNotification: Endpoint[
    ApiKeyCredentials,
    RequestContext,
    ErrorResponse,
    WalletDetail,
    Any
  ] = baseWalletNotificationEndpoint.post
    .out(
      statusCode(StatusCode.Created).description("A new wallet notification has been created")
    )
    .out(jsonBody[WalletDetail])
    .errorOut(EndpointOutputs.basicFailuresAndForbidden)
    .name("createWalletNotification")
    .summary("Create a new wallet notification")

}
