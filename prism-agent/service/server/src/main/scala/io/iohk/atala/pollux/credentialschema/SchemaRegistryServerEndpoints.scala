package io.iohk.atala.pollux.credentialschema

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.model.{Order, PaginationInput}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic
import io.iohk.atala.pollux.credentialschema.SchemaRegistryEndpoints.*
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaController
import io.iohk.atala.pollux.credentialschema.http.{CredentialSchemaInput, FilterInput}
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

import java.util.UUID

class SchemaRegistryServerEndpoints(
    credentialSchemaController: CredentialSchemaController,
    authenticator: Authenticator[BaseEntity]
) {
  def throwableToInternalServerError(throwable: Throwable) =
    ZIO.fail[ErrorResponse](ErrorResponse.internalServerError(detail = Option(throwable.getMessage)))

  val createSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    createSchemaEndpoint
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic {
        case entity: Entity => { case (ctx: RequestContext, schemaInput: CredentialSchemaInput) =>
          credentialSchemaController
            .createSchema(schemaInput)(ctx)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val updateSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    updateSchemaEndpoint
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic {
        case entity: Entity => {
          case (ctx: RequestContext, author: String, id: UUID, schemaInput: CredentialSchemaInput) =>
            credentialSchemaController
              .updateSchema(author, id, schemaInput)(ctx)
              .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val getSchemaByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getSchemaByIdEndpoint
      .zServerLogic { case (ctx: RequestContext, guid: UUID) =>
        credentialSchemaController.getSchemaByGuid(guid)(ctx)
      }

  val lookupSchemasByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupSchemasByQueryEndpoint
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic {
        case entity: Entity => {
          case (
                ctx: RequestContext,
                filter: FilterInput,
                paginationInput: PaginationInput,
                order: Option[Order]
              ) =>
            credentialSchemaController
              .lookupSchemas(
                filter,
                paginationInput.toPagination,
                order
              )(ctx)
              .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val testServerEndpoint: ZServerEndpoint[Any, Any] =
    testEndpoint
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic {
        case entity: Entity => { case requestContext: RequestContext =>
          ZIO.succeed(requestContext.request.toString + " " + entity.walletAccessContext.toString)
        }
      }

  val all: List[ZServerEndpoint[Any, Any]] =
    List(
      createSchemaServerEndpoint,
      updateSchemaServerEndpoint,
      getSchemaByIdServerEndpoint,
      lookupSchemasByQueryServerEndpoint,
      testServerEndpoint
    )
}

object SchemaRegistryServerEndpoints {
  def all: URIO[CredentialSchemaController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
      schemaRegistryService <- ZIO.service[CredentialSchemaController]
      schemaRegistryEndpoints = new SchemaRegistryServerEndpoints(
        schemaRegistryService,
        authenticator
      )
    } yield schemaRegistryEndpoints.all
  }
}
