package org.hyperledger.identus.iam.authentication.apikey

import org.hyperledger.identus.agent.walletapi.model.Entity
import org.hyperledger.identus.agent.walletapi.model.Wallet
import org.hyperledger.identus.agent.walletapi.service.EntityService
import org.hyperledger.identus.agent.walletapi.service.WalletManagementService
import org.hyperledger.identus.iam.authentication.AuthenticationError
import org.hyperledger.identus.iam.authentication.AuthenticationError.*
import org.hyperledger.identus.shared.crypto.Sha256Hash
import org.hyperledger.identus.shared.models.WalletAdministrationContext
import org.hyperledger.identus.shared.models.WalletId
import zio.IO
import zio.URLayer
import zio.ZIO
import zio.ZLayer

import java.util.UUID
import scala.language.implicitConversions
import scala.util.Try

case class ApiKeyAuthenticatorImpl(
    apiKeyConfig: ApiKeyConfig,
    repository: AuthenticationRepository,
    entityService: EntityService,
    walletManagementService: WalletManagementService
) extends ApiKeyAuthenticator {

  override def isEnabled: Boolean = apiKeyConfig.enabled

  override def authenticate(apiKey: String): IO[AuthenticationError, Entity] = {
    if (apiKeyConfig.enabled) {
      if (apiKeyConfig.authenticateAsDefaultUser) {
        ZIO.succeed(Entity.Default)
      } else {
        authenticateBy(apiKey)
          .catchSome {
            case AuthenticationRepositoryError.AuthenticationNotFound(method, secret)
                if apiKeyConfig.autoProvisioning =>
              provisionNewEntity(apiKey)
          }
          .mapError {
            case AuthenticationRepositoryError.AuthenticationNotFound(method, secret) =>
              InvalidCredentials("Invalid API key")
            case AuthenticationRepositoryError.StorageError(cause) =>
              UnexpectedError("Internal error")
            case AuthenticationRepositoryError.UnexpectedError(cause) =>
              UnexpectedError("Internal error")
            case AuthenticationRepositoryError.ServiceError(message) =>
              UnexpectedError("Internal error")
            case AuthenticationRepositoryError.AuthenticationCompromised(entityId, amt, secret) =>
              InvalidCredentials("API key is compromised")
          }
      }
    } else {
      ZIO.fail(
        AuthenticationMethodNotEnabled(s"Authentication method not enabled: ${AuthenticationMethodType.ApiKey.value}")
      )
    }
  }

  protected[apikey] def provisionNewEntity(apiKey: String): IO[AuthenticationRepositoryError, Entity] = synchronized {
    for {
      wallet <- walletManagementService
        .createWallet(Wallet("Auto provisioned wallet", WalletId.random))
        .mapError(cause => AuthenticationRepositoryError.UnexpectedError(cause))
        .provide(ZLayer.succeed(WalletAdministrationContext.Admin()))
      entityToCreate = Entity(name = "Auto provisioned entity", walletId = wallet.id.toUUID)
      entity <- entityService
        .create(entityToCreate)
        .mapError(entityServiceError => AuthenticationRepositoryError.ServiceError(entityServiceError.message))
      _ <- add(entity.id, apiKey)
        .mapError(are => AuthenticationRepositoryError.ServiceError(are.message))
    } yield entity
  }

  protected[apikey] def authenticateBy(apiKey: String): IO[AuthenticationRepositoryError, Entity] = {
    for {
      saltAndApiKey <- ZIO.succeed(apiKeyConfig.salt + apiKey)
      secret <- ZIO
        .fromTry(Try(Sha256Hash.compute(saltAndApiKey.getBytes).hexEncoded))
        .logError("Failed to compute SHA256 hash")
        .mapError(cause => AuthenticationRepositoryError.UnexpectedError(cause))
      entityId <- repository
        .getEntityIdByMethodAndSecret(AuthenticationMethodType.ApiKey, secret)
      entity <- entityService
        .getById(entityId)
        .mapError(entityServiceError => AuthenticationRepositoryError.ServiceError(entityServiceError.message))
    } yield entity
  }

  override def add(entityId: UUID, apiKey: String): IO[AuthenticationError, Unit] = {
    for {
      saltAndApiKey <- ZIO.succeed(apiKeyConfig.salt + apiKey)
      secret <- ZIO
        .fromTry(Try(Sha256Hash.compute(saltAndApiKey.getBytes).hexEncoded))
        .logError("Failed to compute SHA256 hash")
        .mapError(cause => AuthenticationError.UnexpectedError(cause.getMessage))
      _ <- repository
        .insert(entityId, AuthenticationMethodType.ApiKey, secret)
        .logError(s"Insert operation failed for entityId: $entityId")
        .mapError(are => AuthenticationError.UnexpectedError(are.message))
    } yield ()
  }

  override def delete(entityId: UUID, apiKey: String): IO[AuthenticationError, Unit] = {
    for {
      saltAndApiKey <- ZIO.succeed(apiKeyConfig.salt + apiKey)
      secret <- ZIO
        .fromTry(Try(Sha256Hash.compute(saltAndApiKey.getBytes).hexEncoded))
        .logError("Failed to compute SHA256 hash")
        .mapError(cause => AuthenticationError.UnexpectedError(cause.getMessage))
      _ <- repository
        .delete(entityId, AuthenticationMethodType.ApiKey, secret)
        .mapError(are => AuthenticationError.UnexpectedError(are.message))
    } yield ()
  }
}

object ApiKeyAuthenticatorImpl {
  val layer: URLayer[
    ApiKeyConfig & AuthenticationRepository & EntityService & WalletManagementService,
    ApiKeyAuthenticator
  ] =
    ZLayer.fromZIO {
      for {
        apiKeyConfig <- ZIO.service[ApiKeyConfig]
        repository <- ZIO.service[AuthenticationRepository]
        entityService <- ZIO.service[EntityService]
        walletManagementService <- ZIO.service[WalletManagementService]
      } yield ApiKeyAuthenticatorImpl(apiKeyConfig, repository, entityService, walletManagementService)
    }
}
