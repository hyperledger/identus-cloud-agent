package io.iohk.atala.iam.wallet.http

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.admin.AdminApiKeyCredentials
import io.iohk.atala.iam.authentication.admin.AdminApiKeySecurityLogic
import io.iohk.atala.iam.wallet.http.controller.WalletManagementController
import sttp.tapir.ztapir.*
import zio.*

class WalletManagementServerEndpoints(controller: WalletManagementController, authenticator: Authenticator) {

  private def adminApiSecurityLogic(credentials: AdminApiKeyCredentials): IO[ErrorResponse, Entity] =
    AdminApiKeySecurityLogic.securityLogic(credentials)(authenticator)

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

  def all: List[ZServerEndpoint[Any, Any]] = List(
    listWalletServerEndpoint,
    getWalletServerEndpoint,
    createWalletServerEndpoint
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
