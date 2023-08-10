package io.iohk.atala.iam.entity.http

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.iam.entity.http.EntityEndpoints.*
import io.iohk.atala.iam.entity.http.controller.EntityController
import io.iohk.atala.iam.entity.http.model.{CreateEntityRequest, UpdateEntityNameRequest, UpdateEntityWalletIdRequest}
import sttp.tapir.ztapir.*

import java.util.UUID

class EntityServerEndpoints(entityController: EntityController) {

  val createEntityServerEndpoint = createEntityEndpoint.zServerLogic {
    case (rc: RequestContext, request: CreateEntityRequest) =>
      entityController.createEntity(request)(rc)
  }

  val updateEntityNameServerEndpoint = updateEntityNameEndpoint.zServerLogic {
    case (rc: RequestContext, id: UUID, request: UpdateEntityNameRequest) =>
      entityController.updateEntityName(id, request.name)(rc)
  }

  val updateEntityWalletIdServerEndpoint = updateEntityWalletIdEndpoint.zServerLogic {
    case (rc: RequestContext, id: UUID, request: UpdateEntityWalletIdRequest) =>
      entityController.updateEntityWalletId(id, request.walletId)(rc)
  }

  val getEntityByIdServerEndpoint = getEntityByIdEndpoint.zServerLogic { case (rc: RequestContext, id: UUID) =>
    entityController.getEntity(id)(rc)
  }

  val getEntitiesServerEndpoint = getEntitiesEndpoint.zServerLogic {
    case (rc: RequestContext, paginationIn: PaginationInput) =>
      entityController.getEntities(paginationIn)(rc)
  }

  val deleteEntityByIdServerEndpoint = deleteEntityByIdEndpoint.zServerLogic { case (rc: RequestContext, id: UUID) =>
    entityController.deleteEntity(id)(rc)
  }

}
