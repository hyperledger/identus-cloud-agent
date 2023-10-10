package io.iohk.atala.presentproof.controller

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic
import io.iohk.atala.presentproof.controller.PresentProofEndpoints.{
  getAllPresentations,
  getPresentation,
  requestPresentation,
  updatePresentation
}
import io.iohk.atala.presentproof.controller.http.{RequestPresentationAction, RequestPresentationInput}
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

import java.util.UUID

class PresentProofServerEndpoints(
    presentProofController: PresentProofController,
    authenticator: Authenticator[BaseEntity]
) {
  private val requestPresentationEndpoint: ZServerEndpoint[Any, Any] =
    requestPresentation
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, request: RequestPresentationInput) =>
          presentProofController
            .requestPresentation(request)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  private val getAllPresentationsEndpoint: ZServerEndpoint[Any, Any] =
    getAllPresentations
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, paginationInput: PaginationInput, thid: Option[String]) =>
          presentProofController
            .getPresentations(paginationInput, thid)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  private val getPresentationEndpoint: ZServerEndpoint[Any, Any] =
    getPresentation
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, presentationId: UUID) =>
          presentProofController
            .getPresentation(presentationId)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  private val updatePresentationEndpoint: ZServerEndpoint[Any, Any] =
    updatePresentation
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, presentationId: UUID, action: RequestPresentationAction) =>
          presentProofController
            .updatePresentation(presentationId, action)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    requestPresentationEndpoint,
    getAllPresentationsEndpoint,
    getPresentationEndpoint,
    updatePresentationEndpoint
  )
}

object PresentProofServerEndpoints {
  def all: URIO[PresentProofController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
      presentProofController <- ZIO.service[PresentProofController]
      presentProofEndpoints = new PresentProofServerEndpoints(presentProofController, authenticator)
    } yield presentProofEndpoints.all
  }
}
