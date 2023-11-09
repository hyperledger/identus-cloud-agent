package io.iohk.atala.iam.authorization.core

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.agent.walletapi.service.EntityService
import io.iohk.atala.iam.authorization.core.PermissionManagement.Error
import io.iohk.atala.iam.authorization.core.PermissionManagement.Error.ServiceError
import io.iohk.atala.iam.authorization.core.PermissionManagement.Error.WalletNotFoundById
import io.iohk.atala.shared.models.WalletAdministrationContext
import io.iohk.atala.shared.models.WalletId
import zio.*

import scala.language.implicitConversions

class EntityPermissionManagementService(entityService: EntityService) extends PermissionManagement.Service[Entity] {

  override def grantWalletToUser(walletId: WalletId, entity: Entity): ZIO[WalletAdministrationContext, Error, Unit] = {
    for {
      _ <- ZIO
        .serviceWith[WalletAdministrationContext](_.isAuthorized(walletId))
        .filterOrFail(identity)(Error.WalletNotFoundById(walletId))
      _ <- entityService.assignWallet(entity.id, walletId.toUUID).mapError[Error](e => e)
    } yield ()
  }

  override def revokeWalletFromUser(walletId: WalletId, entity: Entity): ZIO[WalletAdministrationContext, Error, Unit] =
    ZIO.fail(Error.ServiceError(s"Revoking wallet permission for an Entity is not yet supported."))

  override def listWalletPermissions(entity: Entity): ZIO[WalletAdministrationContext, Error, Seq[WalletId]] = {
    val walletId = WalletId.fromUUID(entity.walletId)
    ZIO
      .serviceWith[WalletAdministrationContext](_.isAuthorized(walletId))
      .filterOrFail(identity)(Error.WalletNotFoundById(walletId))
      .as(Seq(walletId))
  }

}

object EntityPermissionManagementService {
  val layer: URLayer[EntityService, PermissionManagement.Service[Entity]] =
    ZLayer.fromFunction(EntityPermissionManagementService(_))
}
