package io.iohk.atala.pollux.credentialschema

import io.iohk.atala.api.http.model.{Order, PaginationInput}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic
import io.iohk.atala.pollux.credentialschema.VerificationPolicyEndpoints.*
import io.iohk.atala.pollux.credentialschema.controller.VerificationPolicyController
import io.iohk.atala.pollux.credentialschema.http.{VerificationPolicy, VerificationPolicyInput}
import io.iohk.atala.shared.models.WalletAccessContext
import java.util.UUID
import sttp.tapir.ztapir.*
import zio.*

class VerificationPolicyServerEndpoints(controller: VerificationPolicyController, authenticator: Authenticator) {
  def throwableToInternalServerError(throwable: Throwable) =
    ZIO.fail[ErrorResponse](ErrorResponse.internalServerError(detail = Option(throwable.getMessage)))

  // TODO: make the endpoint typed ZServerEndpoint[SchemaRegistryService, Any]
  val createVerificationPolicyServerEndpoint: ZServerEndpoint[Any, Any] =
    createVerificationPolicyEndpoint
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, input: VerificationPolicyInput) =>
          controller
            .createVerificationPolicy(ctx, input)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val updateVerificationPolicyServerEndpoint: ZServerEndpoint[Any, Any] = {
    updateVerificationPolicyEndpoint
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        {
          case (
                ctx: RequestContext,
                id: UUID,
                nonce: Int,
                update: VerificationPolicyInput
              ) =>
            controller
              .updateVerificationPolicyById(ctx, id, nonce, update)
              .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }
  }

  val getVerificationPolicyByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getVerificationPolicyByIdEndpoint
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, id: UUID) =>
          controller
            .getVerificationPolicyById(ctx, id)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val deleteVerificationPolicyByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    deleteVerificationPolicyByIdEndpoint
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (ctx: RequestContext, id: UUID) =>
          controller
            .deleteVerificationPolicyById(ctx, id)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val lookupVerificationPoliciesByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupVerificationPoliciesByQueryEndpoint
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        {
          case (
                ctx: RequestContext,
                filter: VerificationPolicy.Filter,
                paginationInput: PaginationInput,
                order: Option[Order]
              ) =>
            controller
              .lookupVerificationPolicies(
                ctx,
                filter,
                paginationInput.toPagination,
                order
              )
              .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val all: List[ZServerEndpoint[Any, Any]] =
    List(
      createVerificationPolicyServerEndpoint,
      getVerificationPolicyByIdServerEndpoint,
      updateVerificationPolicyServerEndpoint,
      deleteVerificationPolicyByIdServerEndpoint,
      lookupVerificationPoliciesByQueryServerEndpoint
    )
}

object VerificationPolicyServerEndpoints {
  def all: URIO[VerificationPolicyController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
      controller <- ZIO.service[VerificationPolicyController]
      endpoints = new VerificationPolicyServerEndpoints(controller, authenticator)
    } yield endpoints.all
  }
}
