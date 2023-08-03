package io.iohk.atala.anoncred.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.anoncred.controller.AnoncredEndpoints.*
import io.iohk.atala.anoncred.controller.http.{CreateAnoncredRecordRequest}
import io.iohk.atala.anoncred.controller.http.AcceptAnoncredOfferRequest
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}
import io.iohk.atala.anoncred.controller.AnoncredEndpoints

class AnoncredServerEndpoints(AnoncredController: AnoncredController) {

  val createCredentialOfferEndpoint: ZServerEndpoint[Any, Any] =
    createCredentialOffer.zServerLogic { case (ctx: RequestContext, request: CreateAnoncredRecordRequest) =>
      AnoncredController.createCredentialOffer(request)(ctx)
    }

  val getCredentialRecordsEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialRecords.zServerLogic {
      case (ctx: RequestContext, paginationInput: PaginationInput, thid: Option[String]) =>
        AnoncredController.getCredentialRecords(paginationInput, thid)(ctx)
    }

  val getCredentialRecordEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialRecord.zServerLogic { case (ctx: RequestContext, recordId: String) =>
      AnoncredController.getCredentialRecord(recordId)(ctx)
    }

  val acceptCredentialOfferEndpoint: ZServerEndpoint[Any, Any] =
    acceptCredentialOffer.zServerLogic {
      case (ctx: RequestContext, recordId: String, request: AcceptAnoncredOfferRequest) =>
        AnoncredController.acceptCredentialOffer(recordId, request)(ctx)
    }

  val issueCredentialEndpoint: ZServerEndpoint[Any, Any] =
    issueCredential.zServerLogic { case (ctx: RequestContext, recordId: String) =>
      AnoncredController.issueCredential(recordId)(ctx)
    }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    createCredentialOfferEndpoint,
    getCredentialRecordsEndpoint,
    getCredentialRecordEndpoint,
    acceptCredentialOfferEndpoint,
    issueCredentialEndpoint
  )

}

object AnoncredServerEndpoints {
  def all: URIO[AnoncredController, List[ZServerEndpoint[Any, Any]]] = {
    for {
      anoncredController <- ZIO.service[AnoncredController]
      anoncredEndpoints = new AnoncredServerEndpoints(anoncredController)
    } yield anoncredEndpoints.all
  }
}
