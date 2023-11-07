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
import io.iohk.atala.iam.wallet.http.model.WalletDetail
import io.iohk.atala.iam.wallet.http.model.WalletDetailPage
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

import java.util.UUID

object WalletManagementEndpoints {

  private val baseWalletEndpoint = endpoint
    .tag("Wallet Management")
    .securityIn(adminApiKeyHeader)
    .in("wallets")
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  private val baseMyWalletEndpoint = endpoint
    .tag("Wallet Management")
    .securityIn(apiKeyHeader)
    .securityIn(jwtAuthHeader)
    .in("my-wallets")
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val listWallet: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, PaginationInput),
    ErrorResponse,
    WalletDetailPage,
    Any
  ] =
    baseWalletEndpoint.get
      .in(paginationInput)
      .errorOut(EndpointOutputs.basicFailuresAndForbidden)
      .out(statusCode(StatusCode.Ok).description("Successfully list all the wallets"))
      .out(jsonBody[WalletDetailPage])
      .summary("List all wallets")

  val getWallet: Endpoint[
    AdminApiKeyCredentials,
    (RequestContext, UUID),
    ErrorResponse,
    WalletDetail,
    Any
  ] =
    baseWalletEndpoint.get
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
  ] = baseWalletEndpoint.post
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

  val createMyWallet: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, CreateWalletRequest),
    ErrorResponse,
    WalletDetail,
    Any
  ] = baseMyWalletEndpoint.post
    .in(jsonBody[CreateWalletRequest])
    .out(
      statusCode(StatusCode.Created).description("A new wallet has been created")
    )
    .out(jsonBody[WalletDetail])
    .errorOut(EndpointOutputs.basicFailuresAndForbidden)
    .name("createMyWallet")
    .summary("Create a new wallet")
    .description(
      """Create a new wallet with optional to use provided seed.
        |The seed will be used for DID key derivation inside the wallet.""".stripMargin
    )

}
