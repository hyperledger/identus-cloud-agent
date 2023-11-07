package io.iohk.atala.iam.wallet.http

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.SecurityLogic
import io.iohk.atala.iam.authentication.admin.AdminApiKeyCredentials
import io.iohk.atala.iam.authentication.admin.AdminApiKeySecurityLogic
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.iam.wallet.http.controller.WalletManagementController
import sttp.tapir.ztapir.*
import zio.*

class WalletManagementServerEndpoints(
    controller: WalletManagementController,
    authenticator: Authenticator[BaseEntity]
) {

  private def adminApiSecurityLogic(credentials: AdminApiKeyCredentials): IO[ErrorResponse, BaseEntity] =
    AdminApiKeySecurityLogic.securityLogic(credentials)(authenticator)

  private def tenantSecurityLogic(credentials: (ApiKeyCredentials, JwtCredentials)): IO[ErrorResponse, BaseEntity] =
    SecurityLogic
      .authenticateWith[BaseEntity](credentials)(authenticator)
      .map(e => e.fold(identity, identity))

  val listWalletServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.listWallet
      .zServerSecurityLogic(adminApiSecurityLogic)
      .serverLogic { _ => { case (rc, paginationInput) => controller.listWallet(paginationInput)(rc) } }

  val getWalletServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.getWallet
      .zServerSecurityLogic(adminApiSecurityLogic)
      .serverLogic { _ => { case (rc, walletId) => controller.getWallet(walletId)(rc) } }

  val createWalletServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.createWallet
      .zServerSecurityLogic(adminApiSecurityLogic)
      .serverLogic { _ => { case (rc, createWalletRequest) => controller.createWallet(createWalletRequest)(rc) } }

  val createMyWalletServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.createMyWallet
      .zServerSecurityLogic(tenantSecurityLogic)
      .serverLogic { entity =>
        { case (rc, createWalletRequest) => controller.createMyWallet(createWalletRequest)(rc, entity) }
      }

  val listMyWalletServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.listMyWallet
      .zServerSecurityLogic(tenantSecurityLogic)
      .serverLogic { entity => rc => controller.listMyWallet()(rc, entity) }

  def all: List[ZServerEndpoint[Any, Any]] = List(
    listWalletServerEndpoint,
    getWalletServerEndpoint,
    createWalletServerEndpoint,
    createMyWalletServerEndpoint,
    listMyWalletServerEndpoint
  )

}

object WalletManagementServerEndpoints {
  def all: URIO[WalletManagementController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      walletManagementController <- ZIO.service[WalletManagementController]
      auth <- ZIO.service[DefaultAuthenticator]
      walletManagementServerEndpoints = WalletManagementServerEndpoints(walletManagementController, auth)
    } yield walletManagementServerEndpoints.all
  }
}
