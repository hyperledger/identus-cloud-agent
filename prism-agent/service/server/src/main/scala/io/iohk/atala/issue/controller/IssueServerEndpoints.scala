package io.iohk.atala.issue.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.issue.controller.IssueEndpoints.*
import io.iohk.atala.issue.controller.http.{AcceptCredentialOfferRequest, CreateIssueCredentialRecordRequest}
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

class IssueServerEndpoints(issueController: IssueController) {

  val createCredentialOfferEndpoint: ZServerEndpoint[Any, Any] =
    createCredentialOffer.zServerLogic { case (ctx: RequestContext, request: CreateIssueCredentialRecordRequest) =>
        issueController.createCredentialOffer(request)(ctx)
    }

  val getCredentialRecordsEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialRecords.zServerLogic { case (ctx: RequestContext, paginationInput: PaginationInput, thid: Option[String]) =>
      issueController.getCredentialRecords(paginationInput, thid)(ctx)
    }

  val getCredentialRecordEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialRecord.zServerLogic { case (ctx: RequestContext, recordId: String) =>
      issueController.getCredentialRecord(recordId)(ctx)
    }

  val acceptCredentialOfferEndpoint: ZServerEndpoint[Any, Any] =
    acceptCredentialOffer.zServerLogic { case (ctx: RequestContext, recordId: String, request: AcceptCredentialOfferRequest) =>
      issueController.acceptCredentialOffer(recordId, request)(ctx)
    }

  val issueCredentialEndpoint: ZServerEndpoint[Any, Any] =
   issueCredential.zServerLogic { case (ctx: RequestContext, recordId: String) =>
      issueController.issueCredential(recordId)(ctx)
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
  def all: URIO[IssueController, List[ZServerEndpoint[Any, Any]]] = {
    for {
      issueController <- ZIO.service[IssueController]
      issueEndpoints = new IssueServerEndpoints(issueController)
    } yield issueEndpoints.all
  }
}
