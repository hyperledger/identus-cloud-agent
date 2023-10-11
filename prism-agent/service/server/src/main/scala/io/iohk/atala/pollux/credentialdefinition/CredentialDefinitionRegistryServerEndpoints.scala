package io.iohk.atala.pollux.credentialdefinition

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.model.{Order, PaginationInput}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.Authorizer
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.SecurityLogic
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic
import io.iohk.atala.pollux.credentialdefinition
import io.iohk.atala.pollux.credentialdefinition.CredentialDefinitionRegistryEndpoints.*
import io.iohk.atala.pollux.credentialdefinition.controller.CredentialDefinitionController
import io.iohk.atala.pollux.credentialdefinition.http.{CredentialDefinitionInput, FilterInput}
import sttp.tapir.ztapir.*
import zio.*

import java.util.UUID

class CredentialDefinitionRegistryServerEndpoints(
    credentialDefinitionController: CredentialDefinitionController,
    authenticator: Authenticator[BaseEntity] & Authorizer[BaseEntity]
) {
  def throwableToInternalServerError(throwable: Throwable) =
    ZIO.fail[ErrorResponse](ErrorResponse.internalServerError(detail = Option(throwable.getMessage)))

  val createCredentialDefinitionServerEndpoint: ZServerEndpoint[Any, Any] =
    createCredentialDefinitionEndpoint
      .zServerSecurityLogic(SecurityLogic.authorizeWith(_)(authenticator))
      .serverLogic {
        case wac => { case (ctx: RequestContext, credentialDefinitionInput: CredentialDefinitionInput) =>
          credentialDefinitionController
            .createCredentialDefinition(credentialDefinitionInput)(ctx)
            .provideSomeLayer(ZLayer.succeed(wac))
        }
      }

  val getCredentialDefinitionByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialDefinitionByIdEndpoint.zServerLogic { case (ctx: RequestContext, guid: UUID) =>
      credentialDefinitionController.getCredentialDefinitionByGuid(guid)(ctx)
    }

  val getCredentialDefinitionInnerDefinitionByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialDefinitionInnerDefinitionByIdEndpoint.zServerLogic { case (ctx: RequestContext, guid: UUID) =>
      credentialDefinitionController.getCredentialDefinitionInnerDefinitionByGuid(guid)(ctx)
    }

  val lookupCredentialDefinitionsByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupCredentialDefinitionsByQueryEndpoint
      .zServerSecurityLogic(SecurityLogic.authorizeWith(_)(authenticator))
      .serverLogic {
        case wac => {
          case (
                ctx: RequestContext,
                filter: FilterInput,
                paginationInput: PaginationInput,
                order: Option[Order]
              ) =>
            credentialDefinitionController
              .lookupCredentialDefinitions(
                filter,
                paginationInput.toPagination,
                order
              )(ctx)
              .provideSomeLayer(ZLayer.succeed(wac))
        }
      }

  val all: List[ZServerEndpoint[Any, Any]] =
    List(
      createCredentialDefinitionServerEndpoint,
      getCredentialDefinitionByIdServerEndpoint,
      getCredentialDefinitionInnerDefinitionByIdServerEndpoint,
      lookupCredentialDefinitionsByQueryServerEndpoint
    )
}

object CredentialDefinitionRegistryServerEndpoints {
  def all: URIO[CredentialDefinitionController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      credentialDefinitionRegistryService <- ZIO.service[CredentialDefinitionController]
      authenticator <- ZIO.service[DefaultAuthenticator]
      credentialDefinitionRegistryEndpoints = new CredentialDefinitionRegistryServerEndpoints(
        credentialDefinitionRegistryService,
        authenticator
      )
    } yield credentialDefinitionRegistryEndpoints.all
  }
}
