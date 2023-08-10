package io.iohk.atala.iam.entity.http

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.iam.entity.http.EntityEndpoints.*
import io.iohk.atala.iam.entity.http.controller.EntityController
import io.iohk.atala.iam.entity.http.model.{CreateEntityRequest, UpdateEntityNameRequest, UpdateEntityWalletIdRequest}
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

import java.util.UUID

class EntityServerEndpoints(entityController: EntityController) {

  val createEntityServerEndpoint: ZServerEndpoint[Any, Any] = createEntityEndpoint.zServerLogic {
    case (rc: RequestContext, request: CreateEntityRequest) =>
      entityController.createEntity(request)(rc)
  }

  val updateEntityNameServerEndpoint: ZServerEndpoint[Any, Any] = updateEntityNameEndpoint.zServerLogic {
    case (rc: RequestContext, id: UUID, request: UpdateEntityNameRequest) =>
      entityController.updateEntityName(id, request.name)(rc)
  }

  val updateEntityWalletIdServerEndpoint: ZServerEndpoint[Any, Any] = updateEntityWalletIdEndpoint.zServerLogic {
    case (rc: RequestContext, id: UUID, request: UpdateEntityWalletIdRequest) =>
      entityController.updateEntityWalletId(id, request.walletId)(rc)
  }

  val getEntityByIdServerEndpoint: ZServerEndpoint[Any, Any] = getEntityByIdEndpoint.zServerLogic {
    case (rc: RequestContext, id: UUID) =>
      entityController.getEntity(id)(rc)
  }

  val getEntitiesServerEndpoint: ZServerEndpoint[Any, Any] = getEntitiesEndpoint.zServerLogic {
    case (rc: RequestContext, paginationIn: PaginationInput) =>
      entityController.getEntities(paginationIn)(rc)
  }

  val deleteEntityByIdServerEndpoint: ZServerEndpoint[Any, Any] = deleteEntityByIdEndpoint.zServerLogic {
    case (rc: RequestContext, id: UUID) =>
      entityController.deleteEntity(id)(rc)
  }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    createEntityServerEndpoint,
    updateEntityNameServerEndpoint,
    updateEntityWalletIdServerEndpoint,
    getEntityByIdServerEndpoint,
    getEntitiesServerEndpoint,
    deleteEntityByIdServerEndpoint
  )
}

object EntityServerEndpoints {
  def all: URIO[EntityController, List[ZServerEndpoint[Any, Any]]] = {
    for {
      entityController <- ZIO.service[EntityController]
      entityEndpoints = new EntityServerEndpoints(entityController)
    } yield entityEndpoints.all
  }
}
