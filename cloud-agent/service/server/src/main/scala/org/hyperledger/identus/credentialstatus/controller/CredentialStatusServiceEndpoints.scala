package org.hyperledger.identus.credential.status.controller

import org.hyperledger.identus.agent.walletapi.model.BaseEntity
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.iam.authentication.Authenticator
import org.hyperledger.identus.iam.authentication.Authorizer
import org.hyperledger.identus.iam.authentication.DefaultAuthenticator
import org.hyperledger.identus.iam.authentication.SecurityLogic
import org.hyperledger.identus.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*
import org.hyperledger.identus.credential.status.controller.CredentialStatusEndpoints.*
import sttp.model.StatusCode
import org.hyperledger.identus.pollux.core.model.DidCommID

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
