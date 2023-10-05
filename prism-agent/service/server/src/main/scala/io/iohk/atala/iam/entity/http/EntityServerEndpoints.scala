package io.iohk.atala.iam.entity.http

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.admin.{AdminApiKeyCredentials, AdminApiKeySecurityLogic}
import io.iohk.atala.iam.entity.http.EntityEndpoints.*
import io.iohk.atala.iam.entity.http.controller.EntityController
import io.iohk.atala.iam.entity.http.model.{
  ApiKeyAuthenticationRequest,
  CreateEntityRequest,
  UpdateEntityNameRequest,
  UpdateEntityWalletIdRequest
}
import sttp.tapir.ztapir.*
import zio.{IO, URIO, ZIO}

import java.util.UUID

class EntityServerEndpoints(entityController: EntityController, authenticator: Authenticator) {

  private def adminApiSecurityLogic(credentials: AdminApiKeyCredentials): IO[ErrorResponse, Entity] =
    AdminApiKeySecurityLogic.securityLogic(credentials)(authenticator)

  val createEntityServerEndpoint: ZServerEndpoint[Any, Any] = createEntityEndpoint
    .zServerSecurityLogic(adminApiSecurityLogic)
    .serverLogic {
      case entity: Entity => { case (rc: RequestContext, request: CreateEntityRequest) =>
        entityController.createEntity(request)(rc)
      }
    }

  val updateEntityNameServerEndpoint: ZServerEndpoint[Any, Any] = updateEntityNameEndpoint
    .zServerSecurityLogic(adminApiSecurityLogic)
    .serverLogic {
      case entity: Entity => { case (rc: RequestContext, id: UUID, request: UpdateEntityNameRequest) =>
        entityController.updateEntityName(id, request.name)(rc)
      }

    }

  val updateEntityWalletIdServerEndpoint: ZServerEndpoint[Any, Any] = updateEntityWalletIdEndpoint
    .zServerSecurityLogic(adminApiSecurityLogic)
    .serverLogic {
      case entity: Entity => { case (rc: RequestContext, id: UUID, request: UpdateEntityWalletIdRequest) =>
        entityController.updateEntityWalletId(id, request.walletId)(rc)
      }
    }

  val getEntityByIdServerEndpoint: ZServerEndpoint[Any, Any] = getEntityByIdEndpoint
    .zServerSecurityLogic(adminApiSecurityLogic)
    .serverLogic {
      case entity: Entity => { case (rc: RequestContext, id: UUID) =>
        entityController.getEntity(id)(rc)
      }
    }

  val getEntitiesServerEndpoint: ZServerEndpoint[Any, Any] = getEntitiesEndpoint
    .zServerSecurityLogic(adminApiSecurityLogic)
    .serverLogic {
      case entity: Entity => { case (rc: RequestContext, paginationIn: PaginationInput) =>
        entityController.getEntities(paginationIn)(rc)
      }
    }

  val deleteEntityByIdServerEndpoint: ZServerEndpoint[Any, Any] = deleteEntityByIdEndpoint
    .zServerSecurityLogic(adminApiSecurityLogic)
    .serverLogic {
      case entity: Entity => { case (rc: RequestContext, id: UUID) =>
        entityController.deleteEntity(id)(rc)
      }
    }

  val addEntityApiKeyAuthenticationServerEndpoint: ZServerEndpoint[Any, Any] = addEntityApiKeyAuthenticationEndpoint
    .zServerSecurityLogic(adminApiSecurityLogic)
    .serverLogic {
      case entity: Entity => { case (rc: RequestContext, request: ApiKeyAuthenticationRequest) =>
        entityController.addApiKeyAuth(request.entityId, request.apiKey)(rc)
      }
    }

  val deleteEntityApiKeyAuthenticationServerEndpoint: ZServerEndpoint[Any, Any] =
    deleteEntityApiKeyAuthenticationEndpoint
      .zServerSecurityLogic(adminApiSecurityLogic)
      .serverLogic {
        case entity: Entity => { case (rc: RequestContext, request: ApiKeyAuthenticationRequest) =>
          entityController.deleteApiKeyAuth(request.entityId, request.apiKey)(rc)
        }
      }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    createEntityServerEndpoint,
    updateEntityNameServerEndpoint,
    updateEntityWalletIdServerEndpoint,
    getEntityByIdServerEndpoint,
    getEntitiesServerEndpoint,
    deleteEntityByIdServerEndpoint,
    addEntityApiKeyAuthenticationServerEndpoint,
    deleteEntityApiKeyAuthenticationServerEndpoint
  )
}

object EntityServerEndpoints {
  def all: URIO[EntityController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      entityController <- ZIO.service[EntityController]
      auth <- ZIO.service[DefaultAuthenticator]
      entityEndpoints = new EntityServerEndpoints(entityController, auth)
    } yield entityEndpoints.all
  }
}
