package io.iohk.atala.castor.controller

import io.iohk.atala.api.http.RequestContext
import sttp.tapir.ztapir.*
import zio.*

class DIDRegistrarServerEndpoints(didRegistrarController: DIDRegistrarController) {

  private val listManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.listManagedDid.zServerLogic { (rc, paginationInput) =>
      didRegistrarController.listManagedDid(paginationInput)(rc)
    }

  private val createManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.createManagedDid.zServerLogic { (rc, createManagedDidRequest) =>
      didRegistrarController.createManagedDid(createManagedDidRequest)(rc)
    }

  private val getManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.getManagedDid.zServerLogic { (rc, did) =>
      didRegistrarController.getManagedDid(did)(rc)
    }

  private val publishManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.publishManagedDid.zServerLogic { (rc, did) =>
      didRegistrarController.publishManagedDid(did)(rc)
    }

  private val updateManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.updateManagedDid.zServerLogic { (rc, did, updateRequest) =>
      didRegistrarController.updateManagedDid(did, updateRequest)(rc)
    }

  private val deactivateManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.deactivateManagedDid.zServerLogic { (rc, did) =>
      didRegistrarController.deactivateManagedDid(did)(rc)
    }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    listManagedDidServerEndpoint,
    createManagedDidServerEndpoint,
    getManagedDidServerEndpoint,
    publishManagedDidServerEndpoint,
    updateManagedDidServerEndpoint,
    deactivateManagedDidServerEndpoint
  )

}

object DIDRegistrarServerEndpoints {
  def all: URIO[DIDRegistrarController, List[ZServerEndpoint[Any, Any]]] = {
    for {
      didRegistrarController <- ZIO.service[DIDRegistrarController]
      didRegistrarEndpoints = new DIDRegistrarServerEndpoints(didRegistrarController)
    } yield didRegistrarEndpoints.all
  }
}
