package io.iohk.atala.iam.authorization.keycloak.admin

import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.iam.authorization.core.PermissionManagement
import io.iohk.atala.iam.authorization.core.PermissionManagement.Error.*
import io.iohk.atala.shared.models.WalletId
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.representations.idm.authorization.{ResourceRepresentation, UmaPermissionRepresentation}
import zio.ZIO.*
import zio.ZLayer.*
import zio.{IO, Task, URLayer, ZIO, ZLayer}

import java.util.UUID
import scala.jdk.CollectionConverters.*

case class KeycloakPermissionManagementService(
    authzClient: AuthzClient,
    walletManagementService: WalletManagementService
) extends PermissionManagement.Service {

  private def walletResourceName(walletId: WalletId) = s"wallet-${walletId.toUUID.toString}"

  private def policyName(userId: String, resourceId: String) = s"user $userId on wallet $resourceId permission"

  override def grantWalletToUser(walletId: WalletId, userId: UUID): IO[PermissionManagement.Error, Unit] = {
    for {
      walletOpt <- walletManagementService
        .getWallet(walletId)
        .mapError(wmse => ServiceError(wmse.toThrowable.getMessage))

      wallet <- ZIO
        .fromOption(walletOpt)
        .orElseFail(WalletNotFoundById(walletId))

      walletResourceOpt <- findWalletResource(walletId)
        .logError("Error while finding wallet resource")
        .mapError(UnexpectedError.apply)

      walletResource <- ZIO
        .fromOption(walletResourceOpt)
        .orElse(createWalletResource(walletId))
        .logError("Error while creating wallet resource")
        .mapError(UnexpectedError.apply)
      _ <- ZIO.log(s"Wallet resource created ${walletResource.toString}")

      permission <- createResourcePermission(walletResource.getId, userId.toString)
        .mapError(UnexpectedError.apply)

      _ <- ZIO.log(s"Permission created with id ${permission.getId} and name ${permission.getName}")
    } yield ()
  }

  private def permissionDetails(permission: UmaPermissionRepresentation): String = {
    s"""
       |id: ${permission.getId}
       |name: ${permission.getName}
       |scopes: ${permission.getScopes.asScala.mkString(", ")}
       |users: ${permission.getUsers.asScala.mkString(", ")}
       |""".stripMargin
  }

  private def createResourcePermission(resourceId: String, userId: String): Task[UmaPermissionRepresentation] = {
    val policy = UmaPermissionRepresentation()
    policy.setName(policyName(userId, resourceId))
    policy.setUsers(Set(userId).asJava)

    for {
      umaPermissionRepresentation <- ZIO.attemptBlocking(
        authzClient
          .protection()
          .policy(resourceId)
          .create(policy)
      )
    } yield umaPermissionRepresentation
  }

  private def findWalletResource(walletId: WalletId): Task[Option[ResourceRepresentation]] = {
    for {
      walletResourceOrNull <- ZIO.attemptBlocking(
        authzClient.protection().resource().findByName(walletResourceName(walletId))
      )
    } yield Option(walletResourceOrNull)
  }

  private def createWalletResource(walletId: WalletId): Task[ResourceRepresentation] = {
    val walletResource = ResourceRepresentation()
    walletResource.setId(walletId.toUUID.toString)
    walletResource.setUris(Set(s"/wallets/${walletResourceName(walletId)}").asJava)
    walletResource.setName(walletResourceName(walletId))
    walletResource.setOwnerManagedAccess(true)

    for {
      _ <- ZIO.log(s"Creating resource for the wallet ${walletId.toUUID.toString}")
      response <- ZIO.attemptBlocking(
        authzClient
          .protection()
          .resource()
          .create(walletResource)
      )
      resource <- ZIO.attemptBlocking(
        authzClient
          .protection()
          .resource()
          .findById(walletResource.getId)
      )
      _ <- ZIO.log(s"Resource for the wallet created id: ${resource.getId}, name ${resource.getName}")
    } yield resource
  }

  override def revokeWalletFromUser(walletId: WalletId, userId: UUID): IO[PermissionManagement.Error, Unit] = {
    for {
      walletResourceOpt <- findWalletResource(walletId)
        .logError("Error while finding wallet resource")
        .mapError(UnexpectedError.apply)

      walletResource <- ZIO
        .fromOption(walletResourceOpt)
        .orElseFail(WalletResourceNotFoundById(walletId))

      permissionOpt <- ZIO
        .attemptBlocking(
          authzClient
            .protection()
            .policy(walletResource.getId)
            .find(
              policyName(userId.toString, walletResource.getId),
              null,
              0,
              1
            )
        )
        .map(_.asScala.headOption)
        .logError(s"Error while finding permission by name ${policyName(userId.toString, walletResource.getId)}")
        .mapError(UnexpectedError.apply)

      permission <- ZIO
        .fromOption(permissionOpt)
        .orElseFail(PermissionNotFoundById(userId, walletId, walletResource.getId))

      _ <- ZIO
        .attemptBlocking(
          authzClient
            .protection()
            .policy(walletResource.getId)
            .delete(permission.getId)
        )
        .logError(s"Error while deleting permission ${permission.getId}")
        .mapError(UnexpectedError.apply)

      _ <- ZIO.log(
        s"Permission ${permission.getId} deleted for user ${userId.toString} and wallet ${walletResource.getId}"
      )
    } yield ()
  }
}

object KeycloakPermissionManagementService {
  val layer: URLayer[
    AuthzClient & WalletManagementService,
    PermissionManagement.Service
  ] =
    ZLayer.fromFunction(KeycloakPermissionManagementService(_, _))
}
