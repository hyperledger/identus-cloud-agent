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

  private def multiRoleSecurityLogic(
      credentials: (AdminApiKeyCredentials, ApiKeyCredentials, JwtCredentials)
  ): IO[ErrorResponse, BaseEntity] =
    SecurityLogic
      .authenticateWith[BaseEntity](credentials)(authenticator)
      .map(e => e.fold(identity, identity))

  val listWalletServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.listWallet
      .zServerSecurityLogic(multiRoleSecurityLogic)
      .serverLogic { entity => { case (rc, paginationInput) => controller.listWallet(paginationInput)(rc, entity) } }

  val getWalletServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.getWallet
      .zServerSecurityLogic(adminApiSecurityLogic)
      .serverLogic { _ => { case (rc, walletId) => controller.getWallet(walletId)(rc) } }

  val createWalletServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.createWallet
      .zServerSecurityLogic(multiRoleSecurityLogic)
      .serverLogic { entity =>
        { case (rc, createWalletRequest) => controller.createWallet(createWalletRequest)(rc, entity) }
      }

  def all: List[ZServerEndpoint[Any, Any]] = List(
    listWalletServerEndpoint,
    getWalletServerEndpoint,
    createWalletServerEndpoint,
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
