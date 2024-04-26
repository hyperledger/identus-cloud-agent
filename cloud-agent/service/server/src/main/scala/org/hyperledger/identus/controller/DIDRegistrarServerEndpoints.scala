package org.hyperledger.identus.castor.controller

import org.hyperledger.identus.LogUtils.*
import org.hyperledger.identus.agent.walletapi.model.BaseEntity
import org.hyperledger.identus.iam.authentication.Authenticator
import org.hyperledger.identus.iam.authentication.Authorizer
import org.hyperledger.identus.iam.authentication.DefaultAuthenticator
import org.hyperledger.identus.iam.authentication.SecurityLogic
import org.hyperledger.identus.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

class DIDRegistrarServerEndpoints(
    didRegistrarController: DIDRegistrarController,
    authenticator: Authenticator[BaseEntity],
    authorizer: Authorizer[BaseEntity]
) {

  private val listManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.listManagedDid
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac =>
        { case (rc, paginationInput) =>
          didRegistrarController
            .listManagedDid(paginationInput)(rc)
            .provideSomeLayer(ZLayer.succeed(wac))
            .logTrace(rc)
        }
      }

  private val createManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.createManagedDid
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac =>
        { case (rc, createManagedDidRequest) =>
          didRegistrarController
            .createManagedDid(createManagedDidRequest)(rc)
            .provideSomeLayer(ZLayer.succeed(wac))
            .logTrace(rc)
        }
      }

  private val getManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.getManagedDid
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac =>
        { case (rc, did) =>
          didRegistrarController
            .getManagedDid(did)(rc)
            .provideSomeLayer(ZLayer.succeed(wac))
            .logTrace(rc)
        }
      }

  private val publishManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.publishManagedDid
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac =>
        { case (rc, did) =>
          didRegistrarController
            .publishManagedDid(did)(rc)
            .provideSomeLayer(ZLayer.succeed(wac))
            .logTrace(rc)
        }
      }

  private val updateManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.updateManagedDid
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac =>
        { case (rc, did, updateRequest) =>
          didRegistrarController
            .updateManagedDid(did, updateRequest)(rc)
            .provideSomeLayer(ZLayer.succeed(wac))
            .logTrace(rc)
        }
      }

  private val deactivateManagedDidServerEndpoint: ZServerEndpoint[Any, Any] =
    DIDRegistrarEndpoints.deactivateManagedDid
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac =>
        { case (rc, did) =>
          didRegistrarController
            .deactivateManagedDid(did)(rc)
            .provideSomeLayer(ZLayer.succeed(wac))
            .logTrace(rc)
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
      didRegistrarEndpoints = new DIDRegistrarServerEndpoints(didRegistrarController, authenticator, authenticator)
    } yield didRegistrarEndpoints.all
  }
}
