package io.iohk.atala.pollux.dal

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
import io.iohk.atala.pollux.dal.SchemaRegistryRepositoryIntegrationSpec
import io.iohk.atala.pollux.dto.VerifiableCredentialSchema
import io.iohk.atala.test.container.MigrationAspects
import io.iohk.atala.test.container.MigrationAspects.runMigration
import io.iohk.atala.test.container.PostgresTestContainer.{
  hikariConfigLayer,
  postgres,
  transactor
}
import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

import java.time.{OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.io.Source
object SchemaRegistryRepositoryIntegrationSpec extends ZIOSpecDefault {

  private val postgresLayer = postgres()
  private val transactorLayer =
    postgresLayer >>> hikariConfigLayer >>> transactor
  private val testEnvironmentLayer =
    zio.test.testEnvironment ++ postgresLayer ++ transactorLayer

  object Vocabulary {
    val verifiableCredentialTypes =
      Source.fromResource("data/verifiableCredentialTypes.csv").getLines().toSet
  }
  object Generators {
    val schemaId = Gen.uuid
    val schemaName =
      Gen.oneOf(Gen.fromIterable(Vocabulary.verifiableCredentialTypes))
    val schemaVersion = Gen.int(1, 5).map(i => s"$i.0")
    val schemaDescription = Gen.alphaNumericStringBounded(5, 30)
    val schemaAttribute = Gen.alphaNumericStringBounded(3, 9)
    val schemaAttributes = Gen.setOfBounded(1, 4)(schemaAttribute).map(_.toList)
    val schemaAuthored = Gen.offsetDateTime
    val schemaTag: Gen[Any, String] = Gen.alphaNumericStringBounded(3, 5)
    val schemaTags: Gen[Any, List[String]] =
      Gen.setOfBounded(0, 3)(schemaTag).map(_.toList)

    val schema: Gen[Any, VerifiableCredentialSchema] = for {
      name <- schemaName
      id <- schemaId
      version <- schemaVersion
      description <- schemaDescription
      attributes <- schemaAttributes
      tags <- schemaTags
      authored = OffsetDateTime.now(ZoneOffset.UTC)
    } yield VerifiableCredentialSchema(
      id = id,
      name = name,
      version = version,
      description = Some(description),
      attributes = attributes,
      author = "Prism Agent",
      authored = authored,
      tags = tags
    )
  }

  def spec = suite("schema-registry DAL spec")(
    schemaRegistryCRUD,
    schemaRegistryLookup
  ).provideSomeLayerShared(
    testEnvironmentLayer
  ) @@ nondeterministic @@ sequential @@ zio.test.TestAspect.timed
  // @@ migrate("VerifiableCredentialSchema", "filesystem:migration") //TODO: figure out how to make it work

  private def generateVCS() = VerifiableCredentialSchema(
    id = UUID.randomUUID(),
    name = "",
    version = "1.0",
    tags = List("a", "b"),
    description = Some("description"),
    attributes = List("dob", "first_name"),
    author = "Agent",
    authored = OffsetDateTime.now(ZoneOffset.UTC)
  )

  private val numberOfRecords = 10

  private val schemaRegistryCRUD = suite("schema-registy CRUD for DAL")(
    test("insert, findById, update and delete operations") {
      import io.iohk.atala.pollux.dto.VerifiableCredentialSchema.sql
      import io.iohk.atala.pollux.dto.VerifiableCredentialSchema.sql.*
      import io.iohk.atala.pollux.dto.VerifiableCredentialSchema.sql.idiom.*
      for {
        pg <- ZIO.service[PostgreSQLContainer]
        _ <- runMigration(
          pg.jdbcUrl,
          pg.username,
          pg.password,
          schema = "public",
          locations = "classpath:migration"
        )
        tx <- ZIO.service[Transactor[Task]]

        expected = generateVCS()
        _ <- sql.insert(expected).transact(tx)
        actual <- sql.findBy(expected.id).transact(tx).map(_.headOption)

        schemaCreated = assert(actual.get)(equalTo(expected))

        updatedExpected = expected.copy(name = "new name", description = None)
        updatedActual <- sql.update(updatedExpected).transact(tx)
        updatedActual2 <- sql.findBy(expected.id).transact(tx).map(_.headOption)

        schemaUpdated =
          assert(updatedActual)(equalTo(updatedExpected)) &&
            assert(updatedActual2.get)(equalTo(updatedExpected))

        deletedId <- sql.delete(expected.id).transact(tx)
        notFound <- sql.findBy(expected.id).transact(tx).map(_.headOption)

        schemaDeleted =
          assert(deletedId)(equalTo(expected.id)) &&
            assert(notFound)(isNone)

      } yield schemaCreated && schemaUpdated && schemaDeleted
    }
  )

  val schemaRegistryLookup = suite("schema-registry lookup operations")(
    test("insert, findById, update and delete operations") {
      import io.iohk.atala.pollux.dto.VerifiableCredentialSchema.sql
      import io.iohk.atala.pollux.dto.VerifiableCredentialSchema.sql.*
      import io.iohk.atala.pollux.dto.VerifiableCredentialSchema.sql.idiom.*
      for {
        tx <- ZIO.service[Transactor[Task]]

        generatedSchemas <- Generators.schema.runCollectN(5)
        _ <- generatedSchemas
          .map(schema => sql.insert(schema).transact(tx))
          .reduce((l, r) => l.flatMap(_ => r))

        firstActual = generatedSchemas.head
        firstExpected <- sql
          .findBy(firstActual.id)
          .transact(tx)
          .map(_.headOption)

        schemaCreated = assert(firstActual)(equalTo(firstExpected.get))

      } yield schemaCreated
    },
    test("test lookup samples by the author only") {
      createTestCases(LookupDataSample.authorIsDefinedOnly)
    },
    test("test lookup samples by the name only") {
      createTestCases(LookupDataSample.nameIsDefinedOnly)
    },
    test("test lookup samples by the version only") {
      createTestCases(LookupDataSample.versionIsDefinedOnly)
    },
    test("test lookup samples by the tag only") {
      createTestCases(LookupDataSample.tagIsDefinedOnly)
    },
    test("test lookup samples all cases") {
      createTestCases(LookupDataSample.all)
    }
  )

  def createTestCases(useCasePredicate: LookupDataSample => Boolean) = {
    import VerifiableCredentialSchema.sql
    import VerifiableCredentialSchema.sql.*
    import io.getquill.*
    import io.getquill.idiom.*
    for {
      // cleanup table
      tx <- ZIO.service[Transactor[Task]]
      _ <- sql.deleteAll().transact(tx)

      // insert sample records
      generatedSchemas <- Generators.schema.runCollectN(numberOfRecords)
      _ <- generatedSchemas
        .map(schema => sql.insert(schema).transact(tx))
        .reduce((l, r) => l.flatMap(_ => r))

      // generate test samples
      lookupSamples: List[LookupDataSample] = LookupDataSample
        .deriveLookupDataSamplesFrom(generatedSchemas)
        .toList
        .filter(useCasePredicate)

      _ <- Console.printLine(
        s"${lookupSamples.length} lookup samples are prepared for execution"
      )

      // prepare and run samples
      testResultTasks = lookupSamples
        .map(lookupDataSample =>
          sql
            .run(lookupDataSample.lookupQuery)
            .transact(tx)
            .map(expected => lookupDataSample.assertion(expected))
        )

      // compose the results
      testResult <- testResultTasks
        .foldLeft[Task[TestResult]](ZIO.succeed[TestResult](assertTrue(true)))(
          (acc, next) => acc.flatMap(tr => next.map(ntr => ntr && tr))
        )
    } yield testResult
  }
}

case class LookupDataSample(
    authorOpt: Option[String],
    nameOpt: Option[String],
    versionOpt: Option[String],
    tagOpt: Option[String],
    offsetOpt: Option[Int],
    limitOpt: Option[Int],
    expected: List[VerifiableCredentialSchema],
    expectedCount: Int
) {

  import VerifiableCredentialSchema.sql

  lazy val lookupQuery =
    sql.lookup(authorOpt, nameOpt, versionOpt, tagOpt, offsetOpt, limitOpt)

  def assertion(actual: List[VerifiableCredentialSchema]) = {
    assert(actual)(equalTo(expected) ?? s"$this")
  }
}

object LookupDataSample {
  val authorIsDefined: LookupDataSample => Boolean =
    vcs => vcs.authorOpt.isDefined
  val nameIsDefined: LookupDataSample => Boolean =
    vcs => vcs.nameOpt.isDefined
  val versionIsDefined: LookupDataSample => Boolean =
    vcs => vcs.versionOpt.isDefined
  val tagIsDefined: LookupDataSample => Boolean =
    vcs => vcs.tagOpt.isDefined

  def not(f: LookupDataSample => Boolean): LookupDataSample => Boolean = !f(_)

  def and(
      l: LookupDataSample => Boolean,
      r: LookupDataSample => Boolean
  ): LookupDataSample => Boolean =
    vsc => l(vsc) && r(vsc)

  def and(seq: LookupDataSample => Boolean*): LookupDataSample => Boolean =
    seq.reduce((l, r) => and(l, r))

  val authorIsDefinedOnly = and(
    authorIsDefined,
    not(nameIsDefined),
    not(versionIsDefined),
    not(tagIsDefined)
  )
  val nameIsDefinedOnly = and(
    not(authorIsDefined),
    nameIsDefined,
    not(versionIsDefined),
    not(tagIsDefined)
  )
  val versionIsDefinedOnly = and(
    not(authorIsDefined),
    not(nameIsDefined),
    versionIsDefined,
    not(tagIsDefined)
  )
  val tagIsDefinedOnly = and(
    not(authorIsDefined),
    not(nameIsDefined),
    not(versionIsDefined),
    tagIsDefined
  )
  val all: LookupDataSample => Boolean = vsc => true

  def deriveLookupDataSamplesFrom(
      schemas: List[VerifiableCredentialSchema]
  ): Iterable[LookupDataSample] = {
    val actualNames = schemas.groupBy(_.name).keySet
    val actualVersions = schemas.groupBy(_.version).keySet
    val actualTags = schemas.flatMap(_.tags).toSet
    val actualAuthors = schemas.groupBy(_.author).keySet

    def optionVariation[T](input: T) = List(Option.empty[T], Option(input))

    for {
      name <- actualNames
      version <- actualVersions
      tag <- actualTags
      author <- actualAuthors
      nameVariation <- optionVariation(name)
      versionVariation <- optionVariation(version)
      tagVariation <- optionVariation(tag)
      authorVariation <- optionVariation(author)
      expected = schemas
        .filter(s => nameVariation.fold(true)(name => s.name == name))
        .filter(s =>
          versionVariation.fold(true)(version => s.version == version)
        )
        .filter(s => tagVariation.fold(true)(tag => s.tags.contains(tag)))
        .filter(s => authorVariation.fold(true)(author => s.author == author))
    } yield LookupDataSample(
      authorVariation,
      nameVariation,
      versionVariation,
      tagVariation,
      offsetOpt = Some(0),
      limitOpt = Some(1000),
      expected,
      expected.length
    )
  }
}
