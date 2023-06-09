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
import io.iohk.atala.pollux.sql.model.VerifiableCredentialSchema
import io.iohk.atala.pollux.sql.model.VerifiableCredentialSchema.sql
import io.iohk.atala.test.container.MigrationAspects.*
import io.iohk.atala.test.container.PostgresLayer.*
import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

import java.time.{OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.util.concurrent.TimeUnit
import java.util.{UUID, concurrent}
import scala.collection.mutable
import scala.io.Source

object VerifiableCredentialSchemaSqlIntegrationSpec extends ZIOSpecDefault {

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
    val schemaAuthor =
      Gen.int(1000000, 9999999).map(i => s"did:prism:4fb06243213500578f59588de3e1dd9b266ec1b61e43b0ff86ad0712f$i")
    val schemaAuthored = Gen.offsetDateTime
    val schemaTag: Gen[Any, String] = Gen.alphaNumericStringBounded(3, 5)
    val schemaTags: Gen[Any, List[String]] =
      Gen.setOfBounded(0, 3)(schemaTag).map(_.toList)

    val schema: Gen[Any, VerifiableCredentialSchema] = for {
      name <- schemaName
      version <- schemaVersion
      description <- schemaDescription
      attributes <- schemaAttributes
      tags <- schemaTags
      author <- schemaAuthor
      authored = OffsetDateTime.now(ZoneOffset.UTC)
      id = UUID.randomUUID()
    } yield VerifiableCredentialSchema(
      id = id,
      name = name,
      version = version,
      description = Some(description),
      attributes = attributes,
      author = author,
      authored = authored,
      tags = tags
    )

    private val unique = mutable.Set.empty[String]
    val schemaUnique = for {
      _ <- schema // drain the value to evade the Gen from producing the same over and over again
      s <- schema if !unique.contains(s.uniqueConstraintKey)
      _ = unique += s.uniqueConstraintKey
    } yield s
  }

  def spec = (suite("schema-registry DAL spec")(
    schemaRegistryCRUDSuite,
    schemaRegistryLookupSuite
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
    },
    test("insert N generated, findById, ensure constraint is not broken ") {
      for {
        tx <- ZIO.service[Transactor[Task]]
        _ <- sql.deleteAll.transact(tx)

        generatedSchemas <- Generators.schemaUnique.runCollectN(1000)

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
          generatedSchemas.map(schema => sql.insert(schema).transact(tx))
        )

        firstActual = generatedSchemas.head
        firstExpected <- sql
          .findBy(firstActual.id)
          .transact(tx)
          .map(_.headOption)

        schemaCreated = assert(firstActual)(equalTo(firstExpected.get))

        totalCount <- sql.totalCount.transact(tx)
        lookupCount <- sql.lookupCount().transact(tx)

        totalCountIsN = assert(totalCount)(equalTo(generatedSchemas.length))
        lookupCountIsN = assert(lookupCount)(equalTo(generatedSchemas.length))

      } yield allSchemasHaveUniqueId &&
        allSchemasHaveUniqueConstraint &&
        schemaCreated &&
        totalCountIsN && lookupCountIsN
    }
  ) @@ nondeterministic @@ sequential @@ timed

  val schemaRegistryLookupSuite =
    suite("schema-registry lookup operations based on the generated samples")(
      Map(
        "test lookup samples by the author only" -> LookupDataSample.authorIsDefinedOnly,
        "test lookup samples by the name only" -> LookupDataSample.nameIsDefinedOnly,
        "test lookup samples by the version only" -> LookupDataSample.versionIsDefinedOnly,
        "test lookup samples by the tag only" -> LookupDataSample.tagIsDefinedOnly,
        "test lookup samples all cases" -> LookupDataSample.all
      ).map { case (label, predicate) =>
        createTestCases(predicate, label)
      }
    ) @@ nondeterministic @@ sequential @@ timed

  def createTestCases(useCasePredicate: LookupDataSample => Boolean, label: String) =
    test(label) {
      for {
        // cleanup table
        tx <- ZIO.service[Transactor[Task]]
        _ <- sql.deleteAll.transact(tx)

        // insert sample records
        generatedSchemas <- Generators.schemaUnique.runCollectN(magicNumberN)
        _ <- ZIO.collectAll(
          generatedSchemas.map(schema => sql.insert(schema).transact(tx))
        )

        // generate test samples
        lookupSamples: List[LookupDataSample] = LookupDataSample
          .deriveLookupDataSamplesFrom(generatedSchemas)
          .toList
          .filter(useCasePredicate)
          .take(magicNumberN * 10)

        _ <- ZIO.logDebug(
          s"\t\t${lookupSamples.length} lookup samples are prepared for execution"
        )

        // prepare and run samples
        testResultTasks = lookupSamples
          .map(lookupDataSample =>
            lookupDataSample.lookupQuery
              .transact(tx)
              .map(expected => lookupDataSample.assertion(expected))
          )

        testResultCountTasks = lookupSamples
          .map(lookupDataSample =>
            lookupDataSample.lookupCountQuery
              .transact(tx)
              .map(actualCount => assert(actualCount)(equalTo(lookupDataSample.expectedCount) ?? s"$this"))
          )

        // compose the results
        testResults <- ZIO.collectAll(testResultTasks)
        testResultCounts <- ZIO.collectAll(testResultCountTasks)

        testResult = (testResults ++ testResultCounts).reduce(_ && _)
      } yield testResult
    }
}

case class LookupDataSample(
    authorOpt: Option[String],
    nameOpt: Option[String],
    versionOpt: Option[String],
    attributeOpt: Option[String],
    tagOpt: Option[String],
    offsetOpt: Option[Int],
    limitOpt: Option[Int],
    expected: List[VerifiableCredentialSchema],
    expectedCount: Long
) {

  lazy val lookupQuery =
    sql.lookup(
      authorOpt,
      nameOpt,
      versionOpt,
      attributeOpt,
      tagOpt,
      offsetOpt,
      limitOpt
    )

  lazy val lookupCountQuery =
    sql.lookupCount(authorOpt, nameOpt, versionOpt, attributeOpt, tagOpt)

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
  val attributeIsDefined: LookupDataSample => Boolean =
    vcs => vcs.attributeOpt.isDefined
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
    not(attributeIsDefined),
    not(tagIsDefined)
  )
  val nameIsDefinedOnly = and(
    not(authorIsDefined),
    nameIsDefined,
    not(versionIsDefined),
    not(attributeIsDefined),
    not(tagIsDefined)
  )
  val versionIsDefinedOnly = and(
    not(authorIsDefined),
    not(nameIsDefined),
    versionIsDefined,
    not(attributeIsDefined),
    not(tagIsDefined)
  )
  val attributeIsDefinedOnly = and(
    not(authorIsDefined),
    not(nameIsDefined),
    not(versionIsDefined),
    attributeIsDefined,
    not(tagIsDefined)
  )
  val tagIsDefinedOnly = and(
    not(authorIsDefined),
    not(nameIsDefined),
    not(versionIsDefined),
    not(attributeIsDefined),
    tagIsDefined
  )
  val all: LookupDataSample => Boolean = _ => true

  def deriveLookupDataSamplesFrom(
      schemas: List[VerifiableCredentialSchema]
  ): Iterable[LookupDataSample] = {
    val actualNames = schemas.groupBy(_.name).keySet
    val actualVersions = schemas.groupBy(_.version).keySet
    val actualAttributes = schemas.flatMap(_.attributes).toSet
    val actualTags = schemas.flatMap(_.tags).toSet
    val actualAuthors = schemas.groupBy(_.author).keySet

    def optionVariation[T](input: T) = List(Option.empty[T], Option(input))

    for {
      name <- actualNames
      version <- actualVersions
      attr <- actualAttributes
      tag <- actualTags
      author <- actualAuthors
      nameVariation <- optionVariation(name)
      versionVariation <- optionVariation(version)
      attrVariation <- optionVariation(attr)
      tagVariation <- optionVariation(tag)
      authorVariation <- optionVariation(author)
      expected = schemas
        .filter(s =>
          nameVariation
            .fold(true)(name => s.name == name)
        )
        .filter(s =>
          versionVariation
            .fold(true)(version => s.version == version)
        )
        .filter(s =>
          attrVariation
            .fold(true)(attr => s.attributes.contains(attr))
        )
        .filter(s =>
          tagVariation
            .fold(true)(tag => s.tags.contains(tag))
        )
        .filter(s =>
          authorVariation
            .fold(true)(author => s.author == author)
        )
        .sortBy(_.id.toString)
    } yield LookupDataSample(
      authorVariation,
      nameVariation,
      versionVariation,
      attrVariation,
      tagVariation,
      offsetOpt = Some(0),
      limitOpt = Some(1000),
      expected,
      expected.length
    )
  }
}
