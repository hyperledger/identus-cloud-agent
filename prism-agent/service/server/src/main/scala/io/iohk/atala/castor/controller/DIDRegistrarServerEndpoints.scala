package io.iohk.atala.castor.controller

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.SecurityLogic
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

class DIDRegistrarServerEndpoints(
    didRegistrarController: DIDRegistrarController,
    authenticator: Authenticator
) {

  private val listManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.listManagedDid
      .zServerSecurityLogic(SecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (rc, paginationInput) =>
          didRegistrarController
            .listManagedDid(paginationInput)(rc)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  private val createManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.createManagedDid
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (rc, createManagedDidRequest) =>
          didRegistrarController
            .createManagedDid(createManagedDidRequest)(rc)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  private val getManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.getManagedDid
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (rc, did) =>
          didRegistrarController
            .getManagedDid(did)(rc)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  private val publishManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.publishManagedDid
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (rc, did) =>
          didRegistrarController
            .publishManagedDid(did)(rc)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  private val updateManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.updateManagedDid
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (rc, did, updateRequest) =>
          didRegistrarController
            .updateManagedDid(did, updateRequest)(rc)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  private val deactivateManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.deactivateManagedDid
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (rc, did) =>
          didRegistrarController
            .deactivateManagedDid(did)(rc)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
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
  def all: URIO[DIDRegistrarController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
      didRegistrarController <- ZIO.service[DIDRegistrarController]
      didRegistrarEndpoints = new DIDRegistrarServerEndpoints(didRegistrarController, authenticator)
    } yield didRegistrarEndpoints.all
  }
}
