package io.iohk.atala.pollux.schema

import io.iohk.atala.api.http.model.{CollectionStats, Order, PaginationInput}
import io.iohk.atala.api.http.{FailureResponse, InternalServerError, NotFound, RequestContext}
import io.iohk.atala.pollux.schema.VerificationPolicyEndpoints.*
import io.iohk.atala.pollux.schema.controller.VerificationPolicyController
import io.iohk.atala.pollux.schema.model.{VerificationPolicy, VerificationPolicyInput, VerificationPolicyPage}
import io.iohk.atala.pollux.service.VerificationPolicyService
import sttp.tapir.redoc.RedocUIOptions
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.{Task, URIO, ZIO, ZLayer}

import java.util.UUID

class VerificationPolicyServerEndpoints(
    service: VerificationPolicyService
) {
  def throwableToInternalServerError(throwable: Throwable) =
    ZIO.fail[FailureResponse](InternalServerError(throwable.getMessage))

  // TODO: make the endpoint typed ZServerEndpoint[SchemaRegistryService, Any]
  val createVerificationPolicyServerEndpoint: ZServerEndpoint[Any, Any] =
    createVerificationPolicyEndpoint.zServerLogic { case (ctx: RequestContext, input: VerificationPolicyInput) =>
      service
        .createVerificationPolicy(input)
        .foldZIO(throwableToInternalServerError, vp => ZIO.succeed(vp.withBaseUri(ctx.request.uri)))
    }

  val updateVerificationPolicyServerEndpoint: ZServerEndpoint[Any, Any] = {
    updateVerificationPolicyEndpoint.zServerLogic {
      case (ctx: RequestContext, id: String, update: VerificationPolicyInput) =>
        service
          .updateVerificationPolicyById(id, update)
          .foldZIO(
            throwableToInternalServerError,
            {
              case Some(pv) => ZIO.succeed(pv.withUri(ctx.request.uri))
              case None =>
                ZIO.fail[FailureResponse](
                  NotFound(s"Verification policy is not found by $id")
                )
            }
          )
    }
  }

  val getVerificationPolicyByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getVerificationPolicyByIdEndpoint.zServerLogic { case (ctx: RequestContext, id: String) =>
      service
        .getVerificationPolicyById(id)
        .foldZIO(
          throwableToInternalServerError,
          {
            case Some(pv) => ZIO.succeed(pv.withUri(ctx.request.uri))
            case None =>
              ZIO.fail[FailureResponse](
                NotFound(s"Verification policy is not found by $id")
              )
          }
        )
    }

  val deleteVerificationPolicyByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    deleteVerificationPolicyByIdEndpoint.zServerLogic { case (ctx: RequestContext, id: String) =>
      service
        .deleteVerificationPolicyById(id)
        .foldZIO(
          throwableToInternalServerError,
          {
            case Some(_) => ZIO.succeed(())
            case None =>
              ZIO.fail[FailureResponse](
                NotFound(s"Verification policy is not found by $id")
              )
          }
        )
    }

  val lookupVerificationPoliciesByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupVerificationPoliciesByQueryEndpoint.zServerLogic {
      case (
            ctx: RequestContext,
            filter: VerificationPolicy.Filter,
            paginationInput: PaginationInput,
            order: Option[Order]
          ) =>
        service
          .lookupVerificationPolicies(filter, paginationInput.toPagination, order)
          .foldZIO(
            throwableToInternalServerError,
            {
              case (
                    page: VerificationPolicyPage,
                    stats: CollectionStats
                  ) =>
                ZIO.succeed(
                  VerificationPolicyController(
                    ctx,
                    paginationInput.toPagination,
                    page,
                    stats
                  ).result
                )
            }
          )
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
  def all: URIO[VerificationPolicyService, List[ZServerEndpoint[Any, Any]]] = {
    for {
      service <- ZIO.service[VerificationPolicyService]
      endpoints = new VerificationPolicyServerEndpoints(
        service
      )
    } yield endpoints.all
  }
}
