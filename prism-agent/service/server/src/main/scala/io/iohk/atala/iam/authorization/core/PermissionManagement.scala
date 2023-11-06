package io.iohk.atala.iam.authorization.core

import io.iohk.atala.shared.models.WalletId
import zio.IO

import java.util.UUID

object PermissionManagement {
  trait Service {
    def grantWalletToUser(walletId: WalletId, userId: UUID): IO[Error, Unit]
    def revokeWalletFromUser(walletId: WalletId, userId: UUID): IO[Error, Unit]
  }

  trait Error(message: String)

  object Error {
    case class UserNotFoundById(userId: UUID, cause: Option[Throwable] = None)
        extends Error(s"User $userId is not found" + cause.map(t => s" Cause: ${t.getMessage}"))
    case class WalletNotFoundByUserId(userId: UUID) extends Error(s"Wallet for user $userId is not found")

    case class WalletNotFoundById(walletId: WalletId) extends Error(s"Wallet not found by ${walletId.toUUID}")

    case class WalletResourceNotFoundById(walletId: WalletId)
        extends Error(s"Wallet resource not found by ${walletId.toUUID}")

    case class PermissionNotFoundById(userId: UUID, walletId: WalletId, walletResourceId: String)
        extends Error(
          s"Permission not found by userId: $userId, walletId: ${walletId.toUUID}, walletResourceId: $walletResourceId"
        )

    case class UnexpectedError(cause: Throwable) extends Error(cause.getMessage)

    case class ServiceError(message: String) extends Error(message)
  }
}
