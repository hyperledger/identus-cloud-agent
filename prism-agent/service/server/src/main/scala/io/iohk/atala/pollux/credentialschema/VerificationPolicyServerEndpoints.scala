package io.iohk.atala.pollux.credentialschema

import io.iohk.atala.api.http.model.{Order, PaginationInput}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.pollux.credentialschema.VerificationPolicyEndpoints.*
import io.iohk.atala.pollux.credentialschema.controller.VerificationPolicyController
import io.iohk.atala.pollux.credentialschema.http.{VerificationPolicy, VerificationPolicyInput}
import io.iohk.atala.shared.models.WalletAccessContext
import java.util.UUID
import sttp.tapir.ztapir.*
import zio.*

class VerificationPolicyServerEndpoints(
    controller: VerificationPolicyController,
    walletAccessCtx: WalletAccessContext
) {
  def throwableToInternalServerError(throwable: Throwable) =
    ZIO.fail[ErrorResponse](ErrorResponse.internalServerError(detail = Option(throwable.getMessage)))

  // TODO: make the endpoint typed ZServerEndpoint[SchemaRegistryService, Any]
  val createVerificationPolicyServerEndpoint: ZServerEndpoint[Any, Any] =
    createVerificationPolicyEndpoint.zServerLogic { case (ctx: RequestContext, input: VerificationPolicyInput) =>
      controller
        .createVerificationPolicy(ctx, input)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val updateVerificationPolicyServerEndpoint: ZServerEndpoint[Any, Any] = {
    updateVerificationPolicyEndpoint.zServerLogic {
      case (
            ctx: RequestContext,
            id: UUID,
            nonce: Int,
            update: VerificationPolicyInput
          ) =>
        controller
          .updateVerificationPolicyById(ctx, id, nonce, update)
          .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }
  }

  val getVerificationPolicyByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getVerificationPolicyByIdEndpoint.zServerLogic { case (ctx: RequestContext, id: UUID) =>
      controller
        .getVerificationPolicyById(ctx, id)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val deleteVerificationPolicyByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    deleteVerificationPolicyByIdEndpoint.zServerLogic { case (ctx: RequestContext, id: UUID) =>
      controller
        .deleteVerificationPolicyById(ctx, id)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val lookupVerificationPoliciesByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupVerificationPoliciesByQueryEndpoint.zServerLogic {
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
          .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
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
  def all: URIO[VerificationPolicyController & WalletAccessContext, List[ZServerEndpoint[Any, Any]]] = {
    for {
      // FIXME: do not use global wallet context, use context from interceptor instead
      walletAccessCtx <- ZIO.service[WalletAccessContext]
      controller <- ZIO.service[VerificationPolicyController]
      endpoints = new VerificationPolicyServerEndpoints(controller, walletAccessCtx)
    } yield endpoints.all
  }
}
