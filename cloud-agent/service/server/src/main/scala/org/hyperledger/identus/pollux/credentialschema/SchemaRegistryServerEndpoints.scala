package org.hyperledger.identus.pollux.credentialschema

import org.hyperledger.identus.agent.walletapi.model.BaseEntity
import org.hyperledger.identus.api.http.model.{Order, PaginationInput}
import org.hyperledger.identus.api.http.RequestContext
import org.hyperledger.identus.iam.authentication.{Authenticator, Authorizer, DefaultAuthenticator, SecurityLogic}
import org.hyperledger.identus.pollux.credentialschema.controller.CredentialSchemaController
import org.hyperledger.identus.pollux.credentialschema.http.{CredentialSchemaInput, FilterInput}
import org.hyperledger.identus.pollux.credentialschema.SchemaRegistryEndpoints.*
import org.hyperledger.identus.shared.models.WalletAccessContext
import org.hyperledger.identus.LogUtils.*
import sttp.tapir.ztapir.*
import zio.*

import java.util.UUID

class SchemaRegistryServerEndpoints(
    credentialSchemaController: CredentialSchemaController,
    authenticator: Authenticator[BaseEntity],
    authorizer: Authorizer[BaseEntity]
) {
  val createSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    createSchemaEndpoint
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac =>
        { case (ctx: RequestContext, schemaInput: CredentialSchemaInput) =>
          credentialSchemaController
            .createSchema(schemaInput)(ctx)
            .provideSomeLayer(ZLayer.succeed(wac))
            .logTrace(ctx)
        }
      }

  val updateSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    updateSchemaEndpoint
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac =>
        { case (ctx: RequestContext, author: String, id: UUID, schemaInput: CredentialSchemaInput) =>
          credentialSchemaController
            .updateSchema(author, id, schemaInput)(ctx)
            .provideSomeLayer(ZLayer.succeed(wac))
            .logTrace(ctx)
        }
      }

  val getSchemaByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getSchemaByIdEndpoint
      .zServerLogic { case (ctx: RequestContext, guid: UUID) =>
        credentialSchemaController
          .getSchemaByGuid(guid)(ctx)
          .logTrace(ctx)
      }

  val getRawSchemaByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getRawSchemaByIdEndpoint
      .zServerLogic { case (ctx: RequestContext, guid: UUID) =>
        credentialSchemaController.getSchemaJsonByGuid(guid)(ctx)
      }

  val lookupSchemasByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupSchemasByQueryEndpoint
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac =>
        { case (ctx: RequestContext, filter: FilterInput, paginationInput: PaginationInput, order: Option[Order]) =>
          credentialSchemaController
            .lookupSchemas(
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
      createSchemaServerEndpoint,
      updateSchemaServerEndpoint,
      getSchemaByIdServerEndpoint,
      getRawSchemaByIdServerEndpoint,
      lookupSchemasByQueryServerEndpoint
    )
}

object SchemaRegistryServerEndpoints {
  def all: URIO[CredentialSchemaController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
      schemaRegistryService <- ZIO.service[CredentialSchemaController]
      schemaRegistryEndpoints = new SchemaRegistryServerEndpoints(
        schemaRegistryService,
        authenticator,
        authenticator
      )
    } yield schemaRegistryEndpoints.all
  }
}
