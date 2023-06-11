package io.iohk.atala.pollux.sql

import cats.Functor
import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource}
import cats.syntax.functor.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.zaxxer.hikari.HikariConfig
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.getquill.*
import io.getquill.idiom.*
import io.getquill.util.Messages.{QuatTrace, TraceType, traceQuats}
import io.iohk.atala.pollux.sql.model.db.{CredentialSchema, CredentialSchemaSql}
import io.iohk.atala.test.container.MigrationAspects.*
import io.iohk.atala.test.container.PostgresLayer.*
import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.*
import zio.json.ast.Json
import zio.test.Assertion.*
import zio.test.TestAspect.*
import zio.test.{Gen, *}

import java.time.{OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.util.concurrent.TimeUnit
import java.util.{UUID, concurrent}
import scala.collection.mutable
import scala.io.Source

object CredentialSchemaSqlIntegrationSpec extends ZIOSpecDefault {

  private val pgLayer = postgresLayer(verbose = false)
  private val transactorLayer =
    pgLayer >>> hikariConfigLayer >>> transactor
  private val testEnvironmentLayer =
    zio.test.testEnvironment ++ pgLayer ++ transactorLayer

  object Vocabulary {
    val verifiableCredentialTypes =
      Source
        .fromResource("data/verifiableCredentialTypes.csv")
        .getLines()
        .toSet
    val verifiableCredentialClaims = Source
      .fromResource("data/verifiableCredentialClaims.csv")
      .getLines()
      .toSet
  }

  object Generators {
    val schemaId = Gen.uuid
    val schemaName =
      Gen.oneOf(Gen.fromIterable(Vocabulary.verifiableCredentialTypes))

    val schemaVersion = (Gen.int(1, 3) <*> Gen.int(0, 9) <*> Gen.int(0, 100))
      .map { case (major, minor, patch) => s"$major.$minor.$patch" }

    val schemaDescription = Gen.alphaNumericStringBounded(5, 30)

    val schemaAttribute =
      Gen.fromIterable(Vocabulary.verifiableCredentialClaims)
    val schemaAttributes = Gen.setOfBounded(1, 4)(schemaAttribute).map(_.toList)
    val jsonSchema =
      schemaAttributes.map(attributes => Json.Arr(attributes.map(Json.Str(_)): _*))
    val schemaAuthor =
      Gen.int(1000000, 9999999).map(i => s"did:prism:4fb06243213500578f59588de3e1dd9b266ec1b61e43b0ff86ad0712f$i")
    val schemaAuthored = Gen.offsetDateTime

    val schemaTag: Gen[Any, String] = Gen.alphaNumericStringBounded(3, 5)
    val schemaTags: Gen[Any, List[String]] =
      Gen.setOfBounded(0, 3)(schemaTag).map(_.toList)

    val schema: Gen[Any, CredentialSchema] = for {
      name <- schemaName
      version <- schemaVersion
      description <- schemaDescription
      attributes <- schemaAttributes
      schema <- jsonSchema
      tags <- schemaTags
      author <- schemaAuthor
      authored = OffsetDateTime.now(ZoneOffset.UTC)
      id = UUID.randomUUID()
    } yield CredentialSchema(
      guid = id,
      id = id,
      name = name,
      version = version,
      description = description,
      schema = JsonValue(schema),
      `type` = "AnonCreds",
      author = author,
      authored = authored,
      tags = tags
    )

    private val unique = mutable.Set.empty[String]
    val schemaUnique = for {
      _ <-
        schema // drain the value to evade the Gen from producing the same over and over again
      s <- schema if !unique.contains(s.uniqueConstraintKey)
      _ = unique += s.uniqueConstraintKey
    } yield s
  }

  def spec = (suite("schema-registry DAL spec")(
    schemaRegistryCRUDSuite
  ) @@ nondeterministic @@ sequential @@ timed @@ migrate(
    schema = "public",
    paths = "classpath:sql/pollux"
  )).provideSomeLayerShared(testEnvironmentLayer)

  private val magicNumberN = 20

  val schemaRegistryCRUDSuite = suite("schema-registry CRUD operations")(
    test("insert, findById, update and delete operations") {
      for {
        tx <- ZIO.service[Transactor[Task]]

        expected <- Generators.schema.runCollectN(1).map(_.head)
        _ <- CredentialSchemaSql.insert(expected).transact(tx)
        actual <- CredentialSchemaSql
          .findByGUID(expected.guid)
          .transact(tx)
          .map(_.headOption)

        schemaCreated = assert(actual.get)(equalTo(expected))

        updatedExpected = expected.copy(name = "new name")
        updatedActual <- CredentialSchemaSql
          .update(updatedExpected)
          .transact(tx)
        updatedActual2 <- CredentialSchemaSql
          .findByGUID(expected.id)
          .transact(tx)
          .map(_.headOption)

        schemaUpdated =
          assert(updatedActual)(equalTo(updatedExpected)) &&
            assert(updatedActual2.get)(equalTo(updatedExpected))

        deleted <- CredentialSchemaSql.delete(expected.guid).transact(tx)
        notFound <- CredentialSchemaSql
          .findByGUID(expected.guid)
          .transact(tx)
          .map(_.headOption)

        schemaDeleted =
          assert(deleted)(equalTo(updatedExpected)) &&
            assert(notFound)(isNone)

      } yield schemaCreated && schemaUpdated && schemaDeleted
    },
    test("insert N generated, findById, ensure constraint is not broken ") {
      for {
        tx <- ZIO.service[Transactor[Task]]
        _ <- CredentialSchemaSql.deleteAll.transact(tx)

        generatedSchemas <- Generators.schemaUnique.runCollectN(10)

        allSchemasHaveUniqueId = assert(
          generatedSchemas
            .map(_.id)
            .toSet
            .count(_ => true)
        )(equalTo(generatedSchemas.length))

        allSchemasHaveUniqueConstraint = assert(
          generatedSchemas
            .map(_.uniqueConstraintKey)
            .toSet
            .count(_ => true)
        )(equalTo(generatedSchemas.length))

        _ <- ZIO.collectAll(
          generatedSchemas.map(schema => CredentialSchemaSql.insert(schema).transact(tx))
        )

        firstActual = generatedSchemas.head
        firstExpected <- CredentialSchemaSql
          .findByGUID(firstActual.guid)
          .transact(tx)
          .map(_.headOption)

        schemaCreated = assert(firstActual)(equalTo(firstExpected.get))

        totalCount <- CredentialSchemaSql.totalCount.transact(tx)
        lookupCount <- CredentialSchemaSql.lookupCount().transact(tx)

        totalCountIsN = assert(totalCount)(equalTo(generatedSchemas.length))
        lookupCountIsN = assert(lookupCount)(equalTo(generatedSchemas.length))

      } yield allSchemasHaveUniqueId &&
        allSchemasHaveUniqueConstraint &&
        schemaCreated &&
        totalCountIsN && lookupCountIsN
    }
  ) @@ nondeterministic @@ sequential @@ timed
}
