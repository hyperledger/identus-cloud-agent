package org.hyperledger.identus.pollux.credentialdefinition

import org.hyperledger.identus.agent.walletapi.model.BaseEntity
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.api.http.model.{Order, PaginationInput}
import org.hyperledger.identus.iam.authentication.{Authenticator, Authorizer, DefaultAuthenticator, SecurityLogic}
import org.hyperledger.identus.pollux.credentialdefinition
import org.hyperledger.identus.pollux.credentialdefinition.controller.CredentialDefinitionController
import org.hyperledger.identus.pollux.credentialdefinition.http.{CredentialDefinitionInput, FilterInput}
import org.hyperledger.identus.pollux.credentialdefinition.CredentialDefinitionRegistryEndpoints.*
import org.hyperledger.identus.LogUtils.*
import sttp.tapir.ztapir.*
import zio.*

import java.util.UUID

class CredentialDefinitionRegistryServerEndpoints(
    credentialDefinitionController: CredentialDefinitionController,
    authenticator: Authenticator[BaseEntity],
    authorizer: Authorizer[BaseEntity]
) {

  val createCredentialDefinitionServerEndpoint: ZServerEndpoint[Any, Any] =
    createCredentialDefinitionEndpoint
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic {
        case wac => { case (ctx: RequestContext, credentialDefinitionInput: CredentialDefinitionInput) =>
          credentialDefinitionController
            .createCredentialDefinition(credentialDefinitionInput)(ctx)
            .provideSomeLayer(ZLayer.succeed(wac))
            .logTrace(ctx)
        }
      }

  val getCredentialDefinitionByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialDefinitionByIdEndpoint.zServerLogic { case (ctx: RequestContext, guid: UUID) =>
      credentialDefinitionController
        .getCredentialDefinitionByGuid(guid)(ctx)
        .logTrace(ctx)
    }

  val getCredentialDefinitionInnerDefinitionByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getCredentialDefinitionInnerDefinitionByIdEndpoint.zServerLogic { case (ctx: RequestContext, guid: UUID) =>
      credentialDefinitionController
        .getCredentialDefinitionInnerDefinitionByGuid(guid)(ctx)
        .logTrace(ctx)
    }

  val lookupCredentialDefinitionsByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupCredentialDefinitionsByQueryEndpoint
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic {
        case wac => {
          case (ctx: RequestContext, filter: FilterInput, paginationInput: PaginationInput, order: Option[Order]) =>
            credentialDefinitionController
              .lookupCredentialDefinitions(
                filter,
                paginationInput.toPagination,
                order
              )(ctx)
              .provideSomeLayer(ZLayer.succeed(wac))
              .logTrace(ctx)
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
        authenticator,
        authenticator
      )
    } yield credentialDefinitionRegistryEndpoints.all
  }
}
