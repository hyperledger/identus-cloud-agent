package io.iohk.atala.iam.wallet.http

import io.iohk.atala.api.http.EndpointOutputs
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.iam.authentication.admin.AdminApiKeyCredentials
import io.iohk.atala.iam.authentication.admin.AdminApiKeySecurityLogic.adminApiKeyHeader
import io.iohk.atala.iam.wallet.http.model.CreateWalletRequest
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
    .in("wallets")
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val listWallet: Endpoint[
    AdminApiKeyCredentials,
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
    AdminApiKeyCredentials,
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
    AdminApiKeyCredentials,
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

}
