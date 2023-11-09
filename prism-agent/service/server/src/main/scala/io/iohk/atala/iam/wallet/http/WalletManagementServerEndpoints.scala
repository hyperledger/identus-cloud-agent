package io.iohk.atala.iam.wallet.http

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.Authorizer
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.SecurityLogic
import io.iohk.atala.iam.wallet.http.controller.WalletManagementController
import io.iohk.atala.shared.models.WalletAdministrationContext
import sttp.tapir.ztapir.*
import zio.*

class WalletManagementServerEndpoints(
    controller: WalletManagementController,
    authenticator: Authenticator[BaseEntity],
    authorizer: Authorizer[BaseEntity]
) {

  val listWalletServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.listWallet
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAdminWith(_)(authenticator, authorizer))
      .serverLogic {
        case (_, wac) => { case (rc, paginationInput) =>
          controller.listWallet(paginationInput)(rc).provide(ZLayer.succeed(wac))
        }
      }

  val getWalletServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.getWallet
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAdminWith(_)(authenticator, authorizer))
      .serverLogic {
        case (_, wac) => { case (rc, walletId) => controller.getWallet(walletId)(rc).provide(ZLayer.succeed(wac)) }
      }

  val createWalletServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.createWallet
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAdminWith(_)(authenticator, authorizer))
      .serverLogic {
        case (me, wac) => { case (rc, createWalletRequest) =>
          controller.createWallet(createWalletRequest, me)(rc).provide(ZLayer.succeed(wac))
        }
      }

  val createWalletUmaPermissionServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.createWalletUmaPermmission
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAdminWith(_)(authenticator, authorizer))
      .serverLogic {
        case (_, wac) => { case (rc, walletId, request) =>
          controller.createWalletUmaPermission(walletId, request)(rc).provide(ZLayer.succeed(wac))
        }
      }

  val deleteWalletUmaPermissionServerEndpoint: ZServerEndpoint[Any, Any] =
    WalletManagementEndpoints.deleteWalletUmaPermmission
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAdminWith(_)(authenticator, authorizer))
      .serverLogic {
        case (_, wac) => { case (rc, walletId, subject) =>
          controller.deleteWalletUmaPermission(walletId, subject)(rc).provide(ZLayer.succeed(wac))
        }
      }

  def all: List[ZServerEndpoint[Any, Any]] = List(
    listWalletServerEndpoint,
    getWalletServerEndpoint,
    createWalletServerEndpoint,
    createWalletUmaPermissionServerEndpoint,
    deleteWalletUmaPermissionServerEndpoint
  )

}

object WalletManagementServerEndpoints {
  def all: URIO[WalletManagementController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      walletManagementController <- ZIO.service[WalletManagementController]
      auth <- ZIO.service[DefaultAuthenticator]
      walletManagementServerEndpoints = WalletManagementServerEndpoints(walletManagementController, auth, auth)
    } yield walletManagementServerEndpoints.all
  }
}
