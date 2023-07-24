package io.iohk.atala.pollux.credentialschema

import io.iohk.atala.api.http.model.{Order, PaginationInput}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.pollux.credentialschema.SchemaRegistryEndpoints.*
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaController
import io.iohk.atala.pollux.credentialschema.http.{CredentialSchemaInput, FilterInput}
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

import java.util.UUID

class SchemaRegistryServerEndpoints(
    credentialSchemaController: CredentialSchemaController,
    walletAccessCtx: WalletAccessContext
) {
  def throwableToInternalServerError(throwable: Throwable) =
    ZIO.fail[ErrorResponse](ErrorResponse.internalServerError(detail = Option(throwable.getMessage)))

  val createSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    createSchemaEndpoint.zServerLogic { case (ctx: RequestContext, schemaInput: CredentialSchemaInput) =>
      credentialSchemaController
        .createSchema(schemaInput)(ctx)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val updateSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    updateSchemaEndpoint.zServerLogic {
      case (ctx: RequestContext, author: String, id: UUID, schemaInput: CredentialSchemaInput) =>
        credentialSchemaController
          .updateSchema(author, id, schemaInput)(ctx)
          .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val getSchemaByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getSchemaByIdEndpoint.zServerLogic { case (ctx: RequestContext, guid: UUID) =>
      credentialSchemaController
        .getSchemaByGuid(guid)(ctx)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val lookupSchemasByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupSchemasByQueryEndpoint.zServerLogic {
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
          .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  val testServerEndpoint: ZServerEndpoint[Any, Any] =
    testEndpoint.zServerLogic(requestContext => ZIO.succeed(requestContext.request.toString))

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
  def all: URIO[CredentialSchemaController & WalletAccessContext, List[ZServerEndpoint[Any, Any]]] = {
    for {
      // FIXME: do not use global wallet context, use context from interceptor instead
      walletAccessCtx <- ZIO.service[WalletAccessContext]
      schemaRegistryService <- ZIO.service[CredentialSchemaController]
      schemaRegistryEndpoints = new SchemaRegistryServerEndpoints(
        schemaRegistryService,
        walletAccessCtx
      )
    } yield schemaRegistryEndpoints.all
  }
}
