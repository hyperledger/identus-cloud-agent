package io.iohk.atala.iam.authorization.keycloak.admin

import io.iohk.atala.iam.authorization.core.PermissionManagement
import io.iohk.atala.shared.models.WalletId
import org.keycloak.admin.client.Keycloak
import zio.IO

import java.util.UUID

class KeycloakPermissionManagementService(keycloak: Keycloak) extends PermissionManagement.Service {
  override def grantWalletToUser(walletId: WalletId, userId: UUID): IO[PermissionManagement.Error, Unit] = ???

  override def revokeWalletFromUser(walletId: WalletId, userId: UUID): IO[PermissionManagement.Error, Unit] = ???

  override def getWalletForUser(userId: UUID): IO[PermissionManagement.Error, WalletId] = ???
}
