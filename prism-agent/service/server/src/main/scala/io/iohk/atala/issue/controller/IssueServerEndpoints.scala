package io.iohk.atala.issue.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.issue.controller.IssueEndpoints.*
import io.iohk.atala.issue.controller.http.{AcceptCredentialOfferRequest, CreateIssueCredentialRecordRequest}
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

class IssueServerEndpoints(issueController: IssueController, walletAccessCtx: WalletAccessContext) {

  val createCredentialOfferEndpoint: ZServerEndpoint[Any, Any] =
    createCredentialOffer.zServerLogic { case (ctx: RequestContext, request: CreateIssueCredentialRecordRequest) =>
      issueController
        .createCredentialOffer(request)(ctx)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val getCredentialRecordsEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialRecords.zServerLogic {
      case (ctx: RequestContext, paginationInput: PaginationInput, thid: Option[String]) =>
        issueController
          .getCredentialRecords(paginationInput, thid)(ctx)
          .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val getCredentialRecordEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialRecord.zServerLogic { case (ctx: RequestContext, recordId: String) =>
      issueController
        .getCredentialRecord(recordId)(ctx)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val acceptCredentialOfferEndpoint: ZServerEndpoint[Any, Any] =
    acceptCredentialOffer.zServerLogic {
      case (ctx: RequestContext, recordId: String, request: AcceptCredentialOfferRequest) =>
        issueController
          .acceptCredentialOffer(recordId, request)(ctx)
          .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val issueCredentialEndpoint: ZServerEndpoint[Any, Any] =
    issueCredential.zServerLogic { case (ctx: RequestContext, recordId: String) =>
      issueController
        .issueCredential(recordId)(ctx)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    createCredentialOfferEndpoint,
    getCredentialRecordsEndpoint,
    getCredentialRecordEndpoint,
    acceptCredentialOfferEndpoint,
    issueCredentialEndpoint
  )

}

object IssueServerEndpoints {
  def all: URIO[IssueController & WalletAccessContext, List[ZServerEndpoint[Any, Any]]] = {
    for {
      // FIXME: do not use global wallet context, use context from interceptor instead
      walletAccessCtx <- ZIO.service[WalletAccessContext]
      issueController <- ZIO.service[IssueController]
      issueEndpoints = new IssueServerEndpoints(issueController, walletAccessCtx)
    } yield issueEndpoints.all
  }
}
