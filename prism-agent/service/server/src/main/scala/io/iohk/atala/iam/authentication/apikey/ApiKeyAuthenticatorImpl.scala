package io.iohk.atala.iam.authentication.apikey

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.agent.walletapi.service.EntityService
import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.iam.authentication.AuthenticationError.*
import io.iohk.atala.prism.crypto.Sha256
import zio.{IO, ZIO}
import scala.util.Try

class ApiKeyAuthenticatorImpl(
    apiKeyConfig: ApiKeyConfig,
    repository: AuthenticationRepository,
    entityService: EntityService
) extends ApiKeyAuthenticator {

  override def authenticate(apiKey: String): IO[AuthenticationError, Entity] = {
    if (apiKeyConfig.enabled) {
      if (apiKeyConfig.authenticateAsDefaultUser) {
        ZIO.succeed(Entity.Default)
      } else {
        authenticateBy(apiKey)
          .catchSome {
            case AuthenticationRepositoryError.AuthenticationNotFound(method, secret)
                if apiKeyConfig.autoProvisioning =>
              provisionNewEntity(secret)
          }
          .mapError {
            case AuthenticationRepositoryError.AuthenticationNotFound(method, secret) =>
              InvalidCredentials("Invalid API key")
            case AuthenticationRepositoryError.StorageError(cause) =>
              UnexpectedError("Internal error")
            case AuthenticationRepositoryError.UnexpectedError(cause) =>
              UnexpectedError("Internal error")
            case AuthenticationRepositoryError.EntityServiceError(message) =>
              UnexpectedError("Internal error")
          }
      }
    } else {
      ZIO.fail(
        AuthenticationMethodNotEnabled(s"Authentication method not enabled: ${AuthenticationMethodType.ApiKey.value}")
      )
    }
  }

  private def provisionNewEntity(secret: String): IO[AuthenticationRepositoryError, Entity] = synchronized {
    for {
      entityToCreate <- ZIO.succeed(
        Entity(name = "autocreated")
      ) // FIXME: the new entity is created with the default walletId
      entity <- entityService
        .create(entityToCreate)
        .mapError(entityServiceError => AuthenticationRepositoryError.EntityServiceError(entityServiceError.message))
      _ <- repository.insert(entity.id, AuthenticationMethodType.ApiKey, secret)
    } yield entity
  }

  def authenticateBy(apiKey: String): IO[AuthenticationRepositoryError, Entity] = {
    for {
      saltAndApiKey <- ZIO.succeed(apiKeyConfig.salt + apiKey)
      secret <- ZIO
        .fromTry(Try(Sha256.compute(saltAndApiKey.getBytes).getHexValue))
        .logError("Failed to compute SHA256 hash")
        .mapError(cause => AuthenticationRepositoryError.UnexpectedError(cause))
      entityId <- repository
        .getEntityIdByMethodAndSecret(AuthenticationMethodType.ApiKey, secret)
      entity <- entityService
        .getById(entityId)
        .mapError(entityServiceError => AuthenticationRepositoryError.EntityServiceError(entityServiceError.message))
    } yield entity
  }
}
