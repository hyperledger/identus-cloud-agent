package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.AuthenticationError.InvalidCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.util.UUID

object SecurityLogicSpec extends ZIOSpecDefault {

  /** Authenticate if apiKey is the same as entity ID */
  private def testAuthenticator(entity: Entity): Authenticator[Entity] = {
    new Authenticator[Entity] {
      override def isEnabled: Boolean = true
      override def authenticate(credentials: Credentials): IO[AuthenticationError, Entity] = {
        credentials match {
          case ApiKeyCredentials(Some(apiKey)) if apiKey == entity.id.toString() => ZIO.succeed(entity)
          case _ => ZIO.fail(InvalidCredentials("ApiKey key is invalid"))
        }
      }
    }
  }

  private val disabledAuthenticator: Authenticator[Entity] = {
    new Authenticator[Entity] {
      override def isEnabled: Boolean = false
      override def authenticate(credentials: Credentials): IO[AuthenticationError, Entity] =
        ZIO.fail(AuthenticationError.AuthenticationMethodNotEnabled("not enabled"))
    }
  }

  override def spec = suite("SecurityLogicSpec")(
    test("fallback to default entity when all authentication results are disabled") {
      for {
        authenticatedEntity <- SecurityLogic.authenticate(
          ApiKeyCredentials(Some("key-1")),
          ApiKeyCredentials(Some("key-2")),
          ApiKeyCredentials(Some("key-3"))
        )(disabledAuthenticator)
      } yield assert(authenticatedEntity)(isLeft(equalTo(Entity.Default)))
    },
    test("authenticate all credentials until authenticated") {
      val entity = Entity("alice", UUID.randomUUID())
      for {
        authenticatedEntity <- SecurityLogic.authenticate(
          ApiKeyCredentials(Some("key-1")),
          ApiKeyCredentials(Some("key-2")),
          ApiKeyCredentials(Some(entity.id.toString()))
        )(testAuthenticator(entity))
      } yield assert(authenticatedEntity)(isRight(equalTo(entity)))
    },
    test("reject if none of the credentials can be authenticated") {
      val entity = Entity("alice", UUID.randomUUID())
      for {
        exit <- SecurityLogic
          .authenticate(
            ApiKeyCredentials(Some("key-1")),
            ApiKeyCredentials(Some("key-2")),
            ApiKeyCredentials(Some("key-3"))
          )(testAuthenticator(entity))
          .exit
      } yield assert(exit)(fails(hasField("status", _.status, equalTo(sttp.model.StatusCode.Forbidden.code))))
    }
  )

}
