package io.iohk.atala.credentialstatus.controller

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.Authorizer
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.SecurityLogic
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*
import io.iohk.atala.credentialstatus.controller.CredentialStatusEndpoints.*
import sttp.model.StatusCode
import io.iohk.atala.pollux.core.model.DidCommID

import java.util.UUID

class CredentialStatusServiceEndpoints(
    credentialStatusController: CredentialStatusController,
    authenticator: Authenticator[BaseEntity],
    authorizer: Authorizer[BaseEntity]
) {

  private def obfuscateInternalServerError(e: ErrorResponse): ErrorResponse =
    if e.status == StatusCode.InternalServerError.code then e.copy(detail = Some("Something went wrong"))
    else e

  private val getCredentialStatusListById: ZServerEndpoint[Any, Any] =
    getCredentialStatusListEndpoint
      .zServerLogic { case (ctx: RequestContext, id: UUID) =>
        credentialStatusController
          .getStatusListCredentialById(id)(ctx)
          .logError
          .mapError(obfuscateInternalServerError)

      }

  private val revokeCredentialById: ZServerEndpoint[Any, Any] = {
    revokeCredentialByIdEndpoint
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac =>
        { case (ctx: RequestContext, id: DidCommID) =>
          credentialStatusController
            .revokeCredentialById(id)(ctx)
            .logError
            .mapError(obfuscateInternalServerError)
            .provideSomeLayer(ZLayer.succeed(wac))
        }

      }
  }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    getCredentialStatusListById,
    revokeCredentialById
  )
}

object CredentialStatusServiceEndpoints {
  def all: URIO[CredentialStatusController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
      statusListController <- ZIO.service[CredentialStatusController]
      statusLisEndpoints = new CredentialStatusServiceEndpoints(statusListController, authenticator, authenticator)
    } yield statusLisEndpoints.all
  }
}
