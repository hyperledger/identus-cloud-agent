package io.iohk.atala.pollux.schema

import io.iohk.atala.api.http.model.{Order, PaginationInput}
import io.iohk.atala.api.http.{FailureResponse, InternalServerError, NotFound}
import io.iohk.atala.pollux.schema.VerificationPolicyEndpoints.*
import io.iohk.atala.pollux.schema.model.{VerificationPolicy, VerificationPolicyInput}
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
    createVerificationPolicyEndpoint.zServerLogic(input =>
      service
        .createVerificationPolicy(input)
        .foldZIO(throwableToInternalServerError, vp => ZIO.succeed(vp))
    )

  val updateVerificationPolicyServerEndpoint: ZServerEndpoint[Any, Any] = {
    updateVerificationPolicyEndpoint.zServerLogic { case (id: String, update: VerificationPolicyInput) =>
      service
        .updateVerificationPolicyById(id, update)
        .foldZIO(
          throwableToInternalServerError,
          {
            case Some(pv) => ZIO.succeed(pv)
            case None =>
              ZIO.fail[FailureResponse](
                NotFound(s"Verification policy is not found by $id")
              )
          }
        )
    }
  }

  val getVerificationPolicyByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getVerificationPolicyByIdEndpoint.zServerLogic(id =>
      service
        .getVerificationPolicyById(id)
        .foldZIO(
          throwableToInternalServerError,
          {
            case Some(pv) => ZIO.succeed(pv)
            case None =>
              ZIO.fail[FailureResponse](
                NotFound(s"Verification policy is not found by $id")
              )
          }
        )
    )

  val deleteVerificationPolicyByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    deleteVerificationPolicyByIdEndpoint.zServerLogic(id =>
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
    )

  val lookupVerificationPoliciesByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupVerificationPoliciesByQueryEndpoint.zServerLogic {
      case (
            filter: VerificationPolicy.Filter,
            page: PaginationInput,
            order: Option[Order]
          ) =>
        service
          .lookupVerificationPolicies(filter, page, order)
          .foldZIO(
            throwableToInternalServerError,
            pageOfVCS => ZIO.succeed(pageOfVCS)
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
