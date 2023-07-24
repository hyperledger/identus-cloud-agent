package io.iohk.atala.castor.controller

import sttp.tapir.ztapir.*
import zio.*
import io.iohk.atala.shared.models.WalletAccessContext

class DIDRegistrarServerEndpoints(
    didRegistrarController: DIDRegistrarController,
    walletAccessCtx: WalletAccessContext
) {

  private val listManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.listManagedDid.zServerLogic { (rc, paginationInput) =>
      didRegistrarController
        .listManagedDid(paginationInput)(rc)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  private val createManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.createManagedDid.zServerLogic { (rc, createManagedDidRequest) =>
      didRegistrarController
        .createManagedDid(createManagedDidRequest)(rc)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  private val getManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.getManagedDid.zServerLogic { (rc, did) =>
      didRegistrarController
        .getManagedDid(did)(rc)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  private val publishManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.publishManagedDid.zServerLogic { (rc, did) =>
      didRegistrarController
        .publishManagedDid(did)(rc)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  private val updateManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.updateManagedDid.zServerLogic { (rc, did, updateRequest) =>
      didRegistrarController
        .updateManagedDid(did, updateRequest)(rc)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
    }

  private val deactivateManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.deactivateManagedDid.zServerLogic { (rc, did) =>
      didRegistrarController
        .deactivateManagedDid(did)(rc)
        .provideSomeLayer(ZLayer.succeed(walletAccessCtx)) // FIXME
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
  def all: URIO[DIDRegistrarController & WalletAccessContext, List[ZServerEndpoint[Any, Any]]] = {
    for {
      // FIXME: do not use global wallet context, use context from interceptor instead
      walletAccessCtx <- ZIO.service[WalletAccessContext]
      didRegistrarController <- ZIO.service[DIDRegistrarController]
      didRegistrarEndpoints = new DIDRegistrarServerEndpoints(didRegistrarController, walletAccessCtx)
    } yield didRegistrarEndpoints.all
  }
}
