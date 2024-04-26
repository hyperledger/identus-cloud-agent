package org.hyperledger.identus.iam.authorization

import org.hyperledger.identus.agent.walletapi.model.BaseEntity
import org.hyperledger.identus.agent.walletapi.model.Entity
import org.hyperledger.identus.iam.authentication.oidc.KeycloakEntity
import org.hyperledger.identus.iam.authorization.core.PermissionManagement
import org.hyperledger.identus.iam.authorization.core.PermissionManagement.Error
import org.hyperledger.identus.shared.models.WalletAdministrationContext
import org.hyperledger.identus.shared.models.WalletId
import zio.*

class DefaultPermissionManagementService(
    entityPermission: PermissionManagement.Service[Entity],
    keycloakPermission: PermissionManagement.Service[KeycloakEntity]
) extends PermissionManagement.Service[BaseEntity] {

  def grantWalletToUser(walletId: WalletId, entity: BaseEntity): ZIO[WalletAdministrationContext, Error, Unit] = {
    entity match {
      case entity: Entity           => entityPermission.grantWalletToUser(walletId, entity)
      case kcEntity: KeycloakEntity => keycloakPermission.grantWalletToUser(walletId, kcEntity)
    }
  }

  def revokeWalletFromUser(walletId: WalletId, entity: BaseEntity): ZIO[WalletAdministrationContext, Error, Unit] = {
    entity match {
      case entity: Entity           => entityPermission.revokeWalletFromUser(walletId, entity)
      case kcEntity: KeycloakEntity => keycloakPermission.revokeWalletFromUser(walletId, kcEntity)
    }
  }

  def listWalletPermissions(entity: BaseEntity): ZIO[WalletAdministrationContext, Error, Seq[WalletId]] = {
    entity match {
      case entity: Entity           => entityPermission.listWalletPermissions(entity)
      case kcEntity: KeycloakEntity => keycloakPermission.listWalletPermissions(kcEntity)
    }
  }

}

object DefaultPermissionManagementService {
  def layer: URLayer[
    PermissionManagement.Service[KeycloakEntity] & PermissionManagement.Service[Entity],
    PermissionManagement.Service[BaseEntity]
  ] =
    ZLayer.fromFunction(DefaultPermissionManagementService(_, _))
}
