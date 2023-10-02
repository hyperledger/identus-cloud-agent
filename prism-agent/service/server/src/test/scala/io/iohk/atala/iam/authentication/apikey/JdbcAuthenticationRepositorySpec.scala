package io.iohk.atala.iam.authentication.apikey

import io.iohk.atala.shared.test.containers.PostgresTestContainerSupport
import zio.test.{TestAspect, ZIOSpecDefault}
import zio.ZIO
import zio.test.*
import zio.test.TestAspect.*
import zio.test.Assertion.*
import io.iohk.atala.container.util.MigrationAspects.migrate
import zio.Runtime.removeDefaultLoggers

object JdbcAuthenticationRepositorySpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  val entityIdGen = Gen.uuid
  val secretGen = Gen.alphaNumericStringBounded(256, 256)
  val entityIdAndSecretGen = entityIdGen <*> secretGen

  override def spec = {
    val testSuite =
      suite("JdbcAuthenticationRepositorySpec")(
        crudSpec
      ) @@ TestAspect.sequential @@ migrate(
        schema = "public",
        paths = "classpath:sql/agent"
      )

    testSuite
      .provideSomeLayerShared(
        pgContainerLayer >+> systemTransactorLayer >+> JdbcAuthenticationRepository.layer
      )
      .provide(removeDefaultLoggers)
  }

  private val crudSpec = suite("CRUD operations on the AuthenticationRepository")(
    test("create, read, update, delete") {
      check(entityIdAndSecretGen) { case (entityId, secret) =>
        for {
          repository <- ZIO.service[AuthenticationRepository]
          recordId <- repository.insert(entityId, AuthenticationMethodType.ApiKey, secret)
          fetchedEntityId <- repository.getEntityIdByMethodAndSecret(AuthenticationMethodType.ApiKey, secret)
          _ <- repository.deleteByMethodAndEntityId(entityId, AuthenticationMethodType.ApiKey)
          notFoundEntityId <- repository.getEntityIdByMethodAndSecret(AuthenticationMethodType.ApiKey, secret).flip
        } yield assert(entityId)(equalTo(fetchedEntityId)) &&
          assert(notFoundEntityId)(isSubtype[AuthenticationRepositoryError.AuthenticationNotFound](anything))
      }
    },
    test("insert a similar secret must fail") {
      check(entityIdAndSecretGen) { case (entityId, secret) =>
        for {
          repository <- ZIO.service[AuthenticationRepository]
          _ <- repository.insert(entityId, AuthenticationMethodType.ApiKey, secret)
          insertSameSecret <- repository.insert(entityId, AuthenticationMethodType.ApiKey, secret).flip
          authenticationMethod <- repository
            .findAuthenticationMethodByTypeAndSecret(AuthenticationMethodType.ApiKey, secret)
        } yield assert(insertSameSecret)(
          isSubtype[AuthenticationRepositoryError.AuthenticationCompromised](anything)
        ) &&
          assert(authenticationMethod)(isSome(anything)) &&
          assert(authenticationMethod.flatMap(_.deletedAt))(isSome(anything))
      }
    }
  ) @@ samples(1) @@ nondeterministic
}
