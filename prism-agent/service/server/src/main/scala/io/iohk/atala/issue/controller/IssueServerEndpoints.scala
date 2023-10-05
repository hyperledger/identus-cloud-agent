package io.iohk.atala.issue.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic
import io.iohk.atala.issue.controller.IssueEndpoints.*
import io.iohk.atala.issue.controller.http.{AcceptCredentialOfferRequest, CreateIssueCredentialRecordRequest}
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

class IssueServerEndpoints(issueController: IssueController, authenticator: Authenticator) {

  val createCredentialOfferEndpoint: ZServerEndpoint[Any, Any] =
    createCredentialOffer
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, request: CreateIssueCredentialRecordRequest) =>
          issueController
            .createCredentialOffer(request)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val getCredentialRecordsEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialRecords
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, paginationInput: PaginationInput, thid: Option[String]) =>
          issueController
            .getCredentialRecords(paginationInput, thid)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val getCredentialRecordEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialRecord
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, recordId: String) =>
          issueController
            .getCredentialRecord(recordId)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val acceptCredentialOfferEndpoint: ZServerEndpoint[Any, Any] =
    acceptCredentialOffer
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, recordId: String, request: AcceptCredentialOfferRequest) =>
          issueController
            .acceptCredentialOffer(recordId, request)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val issueCredentialEndpoint: ZServerEndpoint[Any, Any] =
    issueCredential
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, recordId: String) =>
          issueController
            .issueCredential(recordId)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
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
