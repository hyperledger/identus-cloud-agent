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
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

import java.util.UUID

class PresentProofServerEndpoints(presentProofController: PresentProofController) {
  private val requestPresentationEndpoint: ZServerEndpoint[Any, Any] =
    requestPresentation.zServerLogic { case (ctx: RequestContext, request: RequestPresentationInput) =>
      presentProofController.requestPresentation(request)(ctx)
    }

  private val getAllPresentationsEndpoint: ZServerEndpoint[Any, Any] =
    getAllPresentations.zServerLogic {
      case (ctx: RequestContext, paginationInput: PaginationInput, thid: Option[String]) =>
        presentProofController.getAllPresentations(paginationInput.offset, paginationInput.limit, thid)(ctx)
    }

  private val getPresentationEndpoint: ZServerEndpoint[Any, Any] =
    getPresentation.zServerLogic { case (ctx: RequestContext, presentationId: UUID) =>
      presentProofController.getPresentation(presentationId)(ctx)
    }

  private val updatePresentationEndpoint: ZServerEndpoint[Any, Any] =
    updatePresentation.zServerLogic {
      case (ctx: RequestContext, presentationId: UUID, action: RequestPresentationAction) =>
        presentProofController.updatePresentation(presentationId, action)(ctx)
    }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    requestPresentationEndpoint,
    getAllPresentationsEndpoint,
    getPresentationEndpoint,
    updatePresentationEndpoint
  )
}

object PresentProofServerEndpoints {
  def all: URIO[PresentProofController, List[ZServerEndpoint[Any, Any]]] = {
    for {
      presentProofController <- ZIO.service[PresentProofController]
      presentProofEndpoints = new PresentProofServerEndpoints(presentProofController)
    } yield presentProofEndpoints.all
  }
}
