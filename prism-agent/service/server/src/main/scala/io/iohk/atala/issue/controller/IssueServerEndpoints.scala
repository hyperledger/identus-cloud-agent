package io.iohk.atala.issue.controller

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.Authorizer
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.SecurityLogic
import io.iohk.atala.issue.controller.IssueEndpoints.*
import io.iohk.atala.issue.controller.http.{AcceptCredentialOfferRequest, CreateIssueCredentialRecordRequest}
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

class IssueServerEndpoints(
    issueController: IssueController,
    authenticator: Authenticator[BaseEntity] & Authorizer[BaseEntity]
) {

  val createCredentialOfferEndpoint: ZServerEndpoint[Any, Any] =
    createCredentialOffer
      .zServerSecurityLogic(SecurityLogic.authorizeWith(_)(authenticator))
      .serverLogic { wac =>
        { case (ctx: RequestContext, request: CreateIssueCredentialRecordRequest) =>
          issueController
            .createCredentialOffer(request)(ctx)
            .provideSomeLayer(ZLayer.succeed(wac))
        }
      }

  val getCredentialRecordsEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialRecords
      .zServerSecurityLogic(SecurityLogic.authorizeWith(_)(authenticator))
      .serverLogic { wac =>
        { case (ctx: RequestContext, paginationInput: PaginationInput, thid: Option[String]) =>
          issueController
            .getCredentialRecords(paginationInput, thid)(ctx)
            .provideSomeLayer(ZLayer.succeed(wac))
        }
      }

  val getCredentialRecordEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialRecord
      .zServerSecurityLogic(SecurityLogic.authorizeWith(_)(authenticator))
      .serverLogic { wac =>
        { case (ctx: RequestContext, recordId: String) =>
          issueController
            .getCredentialRecord(recordId)(ctx)
            .provideSomeLayer(ZLayer.succeed(wac))
        }
      }

  val acceptCredentialOfferEndpoint: ZServerEndpoint[Any, Any] =
    acceptCredentialOffer
      .zServerSecurityLogic(SecurityLogic.authorizeWith(_)(authenticator))
      .serverLogic { wac =>
        { case (ctx: RequestContext, recordId: String, request: AcceptCredentialOfferRequest) =>
          issueController
            .acceptCredentialOffer(recordId, request)(ctx)
            .provideSomeLayer(ZLayer.succeed(wac))
        }
      }

  val issueCredentialEndpoint: ZServerEndpoint[Any, Any] =
    issueCredential
      .zServerSecurityLogic(SecurityLogic.authorizeWith(_)(authenticator))
      .serverLogic { wac =>
        { case (ctx: RequestContext, recordId: String) =>
          issueController
            .issueCredential(recordId)(ctx)
            .provideSomeLayer(ZLayer.succeed(wac))
        }
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
  def all: URIO[IssueController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
      issueController <- ZIO.service[IssueController]
      issueEndpoints = new IssueServerEndpoints(issueController, authenticator)
    } yield issueEndpoints.all
  }
}
