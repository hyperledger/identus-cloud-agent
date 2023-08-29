package io.iohk.atala.pollux.credentialdefinition

import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.Order
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.pollux.credentialdefinition
import io.iohk.atala.pollux.credentialdefinition.CredentialDefinitionRegistryEndpoints.*
import io.iohk.atala.pollux.credentialdefinition.controller.CredentialDefinitionController
import io.iohk.atala.pollux.credentialdefinition.http.CredentialDefinitionInput
import io.iohk.atala.pollux.credentialdefinition.http.FilterInput
import sttp.tapir.ztapir.*
import zio.*

import java.util.UUID

class CredentialDefinitionRegistryServerEndpoints(
    credentialDefinitionController: CredentialDefinitionController
) {
  def throwableToInternalServerError(throwable: Throwable) =
    ZIO.fail[ErrorResponse](ErrorResponse.internalServerError(detail = Option(throwable.getMessage)))

  val createCredentialDefinitionServerEndpoint: ZServerEndpoint[Any, Any] =
    createCredentialDefinitionEndpoint.zServerLogic {
      case (ctx: RequestContext, credentialDefinitionInput: CredentialDefinitionInput) =>
        credentialDefinitionController.createCredentialDefinition(credentialDefinitionInput)(ctx)
    }

  val getCredentialDefinitionByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialDefinitionByIdEndpoint.zServerLogic { case (ctx: RequestContext, guid: UUID) =>
      credentialDefinitionController.getCredentialDefinitionByGuid(guid)(ctx)
    }

  val lookupCredentialDefinitionsByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupCredentialDefinitionsByQueryEndpoint.zServerLogic {
      case (
            ctx: RequestContext,
            filter: FilterInput,
            paginationInput: PaginationInput,
            order: Option[Order]
          ) =>
        credentialDefinitionController.lookupCredentialDefinitions(
          filter,
          paginationInput.toPagination,
          order
        )(ctx)
    }

  val all: List[ZServerEndpoint[Any, Any]] =
    List(
      createCredentialDefinitionServerEndpoint,
      getCredentialDefinitionByIdServerEndpoint,
      lookupCredentialDefinitionsByQueryServerEndpoint
    )
}

object CredentialDefinitionRegistryServerEndpoints {
  def all: URIO[CredentialDefinitionController, List[ZServerEndpoint[Any, Any]]] = {
    for {
      credentialDefinitionRegistryService <- ZIO.service[CredentialDefinitionController]
      credentialDefinitionRegistryEndpoints = new CredentialDefinitionRegistryServerEndpoints(
        credentialDefinitionRegistryService
      )
    } yield credentialDefinitionRegistryEndpoints.all
  }
}
