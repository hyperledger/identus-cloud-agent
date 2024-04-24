package org.hyperledger.identus.iam.authorization.keycloak.admin

import org.hyperledger.identus.agent.walletapi.model.Wallet
import org.hyperledger.identus.agent.walletapi.service.WalletManagementService
import org.hyperledger.identus.iam.authentication.oidc.KeycloakClient
import org.hyperledger.identus.iam.authentication.oidc.KeycloakEntity
import org.hyperledger.identus.iam.authorization.core.PermissionManagement
import org.hyperledger.identus.iam.authorization.core.PermissionManagement.Error
import org.hyperledger.identus.iam.authorization.core.PermissionManagement.Error.*
import org.hyperledger.identus.shared.models.WalletAdministrationContext
import org.hyperledger.identus.shared.models.WalletId
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.representations.idm.authorization.{ResourceRepresentation, UmaPermissionRepresentation}
import zio.*

import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.util.Try

case class KeycloakPermissionManagementService(
    authzClient: AuthzClient,
    keycloakClient: KeycloakClient,
    walletManagementService: WalletManagementService
) extends PermissionManagement.Service[KeycloakEntity] {

  private def walletResourceName(walletId: WalletId) = s"wallet-${walletId.toUUID.toString}"

  private def policyName(userId: String, resourceId: String) = s"user $userId on wallet $resourceId permission"

  override def grantWalletToUser(
      walletId: WalletId,
      entity: KeycloakEntity
  ): ZIO[WalletAdministrationContext, PermissionManagement.Error, Unit] = {
    for {
      _ <- walletManagementService
        .getWallet(walletId)
        .mapError(wmse => ServiceError(wmse.toThrowable.getMessage))
        .someOrFail(WalletNotFoundById(walletId))

      walletResourceOpt <- findWalletResource(walletId)
        .logError("Error while finding wallet resource")
        .mapError(UnexpectedError.apply)

      walletResource <- ZIO
        .fromOption(walletResourceOpt)
        .orElse(createWalletResource(walletId))
        .logError("Error while creating wallet resource")
        .mapError(UnexpectedError.apply)
      _ <- ZIO.log(s"Wallet resource created ${walletResource.toString}")

      permission <- createResourcePermission(walletResource.getId, entity.id.toString)
        .mapError(UnexpectedError.apply)

      _ <- ZIO.log(s"Permission created with id ${permission.getId} and name ${permission.getName}")
    } yield ()
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
      walletResource <- ZIO
        .attemptBlocking(
          authzClient.protection().resource().findById(walletId.toUUID.toString())
        )
        .asSome
        .catchSome { case e: RuntimeException =>
          if (e.getMessage().contains("Could not find resource")) ZIO.none
          else ZIO.fail(e)
        }
    } yield walletResource
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

  override def revokeWalletFromUser(
      walletId: WalletId,
      entity: KeycloakEntity
  ): ZIO[WalletAdministrationContext, PermissionManagement.Error, Unit] = {
    val userId = entity.id
    for {
      _ <- walletManagementService
        .getWallet(walletId)
        .mapError(wmse => ServiceError(wmse.toThrowable.getMessage))
        .someOrFail(WalletNotFoundById(walletId))

      walletResource <- findWalletResource(walletId)
        .logError("Error while finding wallet resource")
        .mapError(UnexpectedError.apply)
        .someOrFail(WalletResourceNotFoundById(walletId))

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

  override def listWalletPermissions(entity: KeycloakEntity): ZIO[WalletAdministrationContext, Error, Seq[WalletId]] = {
    for {
      token <- ZIO
        .fromOption(entity.accessToken)
        .mapError(_ => Error.ServiceError("AccessToken is missing for listing permissions."))
      tokenIsRpt <- ZIO.fromEither(token.isRpt).mapError(Error.ServiceError(_))
      rpt <-
        if (tokenIsRpt) ZIO.succeed(token)
        else if (keycloakClient.keycloakConfig.autoUpgradeToRPT) {
          keycloakClient
            .getRpt(token)
            .logError("Fail to obtail RPT for wallet permissions")
            .mapError(e => Error.ServiceError(e.message))
        } else ZIO.fail(Error.PermissionNotAvailable(entity.id, s"AccessToken is not RPT."))
      permittedResources <- keycloakClient
        .checkPermissions(rpt)
        .logError("Fail to list resource permissions on keycloak")
        .mapError(e => Error.ServiceError(e.message))
      permittedWallet <- getPermittedWallet(permittedResources)
    } yield permittedWallet.map(_.id)
  }

  private def getPermittedWallet(resourceIds: Seq[String]): ZIO[WalletAdministrationContext, Error, Seq[Wallet]] = {
    val walletIds = resourceIds.flatMap(id => Try(UUID.fromString(id)).toOption).map(WalletId.fromUUID)
    walletManagementService
      .getWallets(walletIds)
      .mapError(e => Error.UnexpectedError(e.toThrowable))
  }
}

object KeycloakPermissionManagementService {
  val layer: URLayer[
    AuthzClient & KeycloakClient & WalletManagementService,
    PermissionManagement.Service[KeycloakEntity]
  ] =
    ZLayer.fromFunction(KeycloakPermissionManagementService(_, _, _))

  val disabled: ULayer[PermissionManagement.Service[KeycloakEntity]] =
    ZLayer.succeed {
      val notEnabledError = ZIO.fail(PermissionManagement.Error.ServiceError("Keycloak is not enabled"))
      new PermissionManagement.Service[KeycloakEntity] {
        override def grantWalletToUser(walletId: WalletId, entity: KeycloakEntity): IO[Error, Unit] = notEnabledError
        override def revokeWalletFromUser(walletId: WalletId, entity: KeycloakEntity): IO[Error, Unit] = notEnabledError
        override def listWalletPermissions(entity: KeycloakEntity): IO[Error, Seq[WalletId]] = notEnabledError
      }
    }
}
