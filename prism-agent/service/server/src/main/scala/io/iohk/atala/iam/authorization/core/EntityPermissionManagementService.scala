package io.iohk.atala.iam.authorization.core

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.agent.walletapi.service.EntityService
import io.iohk.atala.iam.authorization.core.PermissionManagement.Error
import io.iohk.atala.shared.models.WalletId
import zio.*

import scala.language.implicitConversions

class EntityPermissionManagementService(entityService: EntityService) extends PermissionManagement.Service[Entity] {

  override def grantWalletToUser(walletId: WalletId, entity: Entity): IO[Error, Unit] = {
    entityService.assignWallet(entity.id, walletId.toUUID).mapError(e => e)
  }

  override def revokeWalletFromUser(walletId: WalletId, entity: Entity): IO[Error, Unit] =
    ZIO.fail(Error.ServiceError(s"Revoking wallet permission for an Entity is not yet supported."))

}

object EntityPermissionManagementService {
  val layer: URLayer[EntityService, PermissionManagement.Service[Entity]] =
    ZLayer.fromFunction(EntityPermissionManagementService(_))
}
