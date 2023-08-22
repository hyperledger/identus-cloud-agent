package io.iohk.atala.presentproof.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
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
    walletAccessCtx: WalletAccessContext
) {
  private val requestPresentationEndpoint: ZServerEndpoint[Any, Any] =
    requestPresentation.zServerLogic { case (ctx: RequestContext, request: RequestPresentationInput) =>
      presentProofController
        .requestPresentation(request)(ctx)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  private val getAllPresentationsEndpoint: ZServerEndpoint[Any, Any] =
    getAllPresentations.zServerLogic {
      case (ctx: RequestContext, paginationInput: PaginationInput, thid: Option[String]) =>
        presentProofController
          .getPresentations(paginationInput, thid)(ctx)
          .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  private val getPresentationEndpoint: ZServerEndpoint[Any, Any] =
    getPresentation.zServerLogic { case (ctx: RequestContext, presentationId: UUID) =>
      presentProofController
        .getPresentation(presentationId)(ctx)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  private val updatePresentationEndpoint: ZServerEndpoint[Any, Any] =
    updatePresentation.zServerLogic {
      case (ctx: RequestContext, presentationId: UUID, action: RequestPresentationAction) =>
        presentProofController
          .updatePresentation(presentationId, action)(ctx)
          .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    requestPresentationEndpoint,
    getAllPresentationsEndpoint,
    getPresentationEndpoint,
    updatePresentationEndpoint
  )
}

object PresentProofServerEndpoints {
  def all: URIO[PresentProofController & WalletAccessContext, List[ZServerEndpoint[Any, Any]]] = {
    for {
      // FIXME: do not use global wallet context, use context from interceptor instead
      walletAccessCtx <- ZIO.service[WalletAccessContext]
      presentProofController <- ZIO.service[PresentProofController]
      presentProofEndpoints = new PresentProofServerEndpoints(presentProofController, walletAccessCtx)
    } yield presentProofEndpoints.all
  }
}
