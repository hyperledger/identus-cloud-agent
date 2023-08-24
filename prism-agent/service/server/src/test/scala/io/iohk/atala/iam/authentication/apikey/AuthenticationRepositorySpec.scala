package io.iohk.atala.iam.authentication.apikey

import io.iohk.atala.shared.test.containers.PostgresTestContainerSupport
import zio.test.{TestAspect, ZIOSpecDefault}

import zio.ZIO
import zio.test.*
import zio.test.TestAspect.*
import zio.test.Assertion.*
import io.iohk.atala.container.util.MigrationAspects.migrate

object AuthenticationRepositorySpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  override def spec = {
    val testSuite =
      suite("JdbcAuthenticationRepositorySpec")(
        crudSpec
      )  @@ TestAspect.sequential @@ migrate(
        schema = "public",
        paths = "filesystem:../server/src/main/resources/sql/agent"
      )

    testSuite.provideSomeLayerShared(
      pgContainerLayer >+> systemTransactorLayer >+> AuthenticationRepositoryImpl.layer
    )
  }

  private val crudSpec = suite("CRUD operations on the AuthenticationRepository")(
    test("create, read, update, delete") {
      check(Gen.uuid <*> Gen.alphaNumericStringBounded(256, 256)) { case (entityId, secret) =>
        for {
          repository <- ZIO.service[AuthenticationRepository]
          recordId <- repository.insert(entityId, AuthenticationMethodType.ApiKey, secret)
          fetchedEntityId <- repository.getEntityIdByMethodAndSecret(AuthenticationMethodType.ApiKey, secret)
          _ <- repository.deleteByMethodAndEntityId(AuthenticationMethodType.ApiKey, entityId)
          notFoundEntityId <- repository.getEntityIdByMethodAndSecret(AuthenticationMethodType.ApiKey, secret).flip
        } yield assert(entityId)(equalTo(fetchedEntityId)) &&
          assert(notFoundEntityId)(isSubtype[AuthenticationRepositoryError.AuthenticationNotFound](anything))
      }
    }
  ) @@ samples(100) @@ nondeterministic
}
