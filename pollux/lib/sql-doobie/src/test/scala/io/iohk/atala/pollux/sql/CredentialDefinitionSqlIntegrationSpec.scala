package io.iohk.atala.pollux.sql

import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import io.getquill.*
import io.iohk.atala.pollux.sql.model.db.CredentialDefinition
import io.iohk.atala.pollux.sql.model.db.CredentialDefinitionSql
import io.iohk.atala.test.container.MigrationAspects.*
import io.iohk.atala.test.container.PostgresLayer.*
import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.*
import zio.json.ast.Json
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import scala.collection.mutable
import scala.io.Source

object CredentialDefinitionSqlIntegrationSpec extends ZIOSpecDefault {

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
    val credentialDefinitionId = Gen.uuid
    val credentialDefinitionName =
      Gen.oneOf(Gen.fromIterable(Vocabulary.verifiableCredentialTypes))

    val credentialDefinitionVersion = (Gen.int(1, 3) <*> Gen.int(0, 9) <*> Gen.int(0, 100))
      .map { case (major, minor, patch) => s"$major.$minor.$patch" }

    val credentialDefinitionDescription = Gen.alphaNumericStringBounded(5, 30)

    val credentialDefinitionAttribute =
      Gen.fromIterable(Vocabulary.verifiableCredentialClaims)
    val credentialDefinitionAttributes = Gen.setOfBounded(1, 4)(credentialDefinitionAttribute).map(_.toList)
    val jsonCredentialDefinition =
      credentialDefinitionAttributes.map(attributes => Json.Arr(attributes.map(Json.Str(_)): _*))

    val keyCorrectnessProofAttribute =
      Gen.fromIterable(Vocabulary.verifiableCredentialClaims)
    val keyCorrectnessProofAttributes = Gen.setOfBounded(1, 4)(credentialDefinitionAttribute).map(_.toList)
    val jsonCorrectnessProof =
      credentialDefinitionAttributes.map(attributes => Json.Arr(attributes.map(Json.Str(_)): _*))

    val credentialDefinitionAuthor =
      Gen.int(1000000, 9999999).map(i => s"did:prism:4fb06243213500578f59588de3e1dd9b266ec1b61e43b0ff86ad0712f$i")
    val credentialDefinitionAuthored = Gen.offsetDateTime

    val credentialDefinitionTag: Gen[Any, String] = Gen.alphaNumericStringBounded(3, 5)
    val credentialDefinitionTags: Gen[Any, List[String]] =
      Gen.setOfBounded(0, 3)(credentialDefinitionTag).map(_.toList)

    val credentialDefinitionSchemaId = Gen.alphaNumericStringBounded(4, 12)
    val keyCorrectnessProofJsonSchemaId = Gen.alphaNumericStringBounded(4, 12)
    val definitionJsonSchemaId = Gen.alphaNumericStringBounded(4, 12)
    val credentialDefinitionSignatureType = Gen.alphaNumericStringBounded(4, 12)
    val credentialDefinitionSupportRevocation = Gen.boolean

    val credentialDefinition: Gen[Any, CredentialDefinition] = for {
      name <- credentialDefinitionName
      version <- credentialDefinitionVersion
      description <- credentialDefinitionDescription
      definitionJsonSchemaId <- definitionJsonSchemaId
      definition <- jsonCredentialDefinition
      keyCorrectnessProofJsonSchemaId <- keyCorrectnessProofJsonSchemaId
      keyCorrectnessProof <- jsonCredentialDefinition
      tags <- credentialDefinitionTags
      author <- credentialDefinitionAuthor
      authored = OffsetDateTime.now(ZoneOffset.UTC)
      id = UUID.randomUUID()
      schemaId <- credentialDefinitionSchemaId
      signatureType <- credentialDefinitionSignatureType
      supportRevocation <- credentialDefinitionSupportRevocation
    } yield CredentialDefinition(
      guid = id,
      id = id,
      name = name,
      version = version,
      description = description,
      author = author,
      authored = authored,
      tags = tags,
      definitionJsonSchemaId = definitionJsonSchemaId,
      definition = JsonValue(definition),
      keyCorrectnessProofJsonSchemaId = keyCorrectnessProofJsonSchemaId,
      keyCorrectnessProof = JsonValue(keyCorrectnessProof),
      schemaId = schemaId,
      signatureType = signatureType,
      supportRevocation = supportRevocation
    )

    private val unique = mutable.Set.empty[String]
    val credentialDefinitionUnique = for {
      _ <-
        credentialDefinition // drain the value to evade the Gen from producing the same over and over again
      s <- credentialDefinition if !unique.contains(s.uniqueConstraintKey)
      _ = unique += s.uniqueConstraintKey
    } yield s
  }

  def spec = (suite("credential-definition-registry DAL spec")(
    credentialDefinitionRegistryCRUDSuite
  ) @@ nondeterministic @@ sequential @@ timed @@ migrate(
    schema = "public",
    paths = "classpath:sql/pollux"
  )).provideSomeLayerShared(testEnvironmentLayer)

  val credentialDefinitionRegistryCRUDSuite = suite("credential-definition-registry CRUD operations")(
    test("insert, findById, update and delete operations") {
      for {
        tx <- ZIO.service[Transactor[Task]]

        expected <- Generators.credentialDefinition.runCollectN(1).map(_.head)
        _ <- CredentialDefinitionSql.insert(expected).transact(tx)
        actual <- CredentialDefinitionSql
          .findByGUID(expected.guid)
          .transact(tx)
          .map(_.headOption)

        credentialDefinitionCreated = assert(actual.get)(equalTo(expected))

        updatedExpected = expected.copy(name = "new name")
        updatedActual <- CredentialDefinitionSql
          .update(updatedExpected)
          .transact(tx)
        updatedActual2 <- CredentialDefinitionSql
          .findByGUID(expected.id)
          .transact(tx)
          .map(_.headOption)

        credentialDefinitionUpdated =
          assert(updatedActual)(equalTo(updatedExpected)) &&
            assert(updatedActual2.get)(equalTo(updatedExpected))

        deleted <- CredentialDefinitionSql.delete(expected.guid).transact(tx)
        notFound <- CredentialDefinitionSql
          .findByGUID(expected.guid)
          .transact(tx)
          .map(_.headOption)

        credentialDefinitionDeleted =
          assert(deleted)(equalTo(updatedExpected)) &&
            assert(notFound)(isNone)

      } yield credentialDefinitionCreated && credentialDefinitionUpdated && credentialDefinitionDeleted
    },
    test("insert N generated, findById, ensure constraint is not broken ") {
      for {
        tx <- ZIO.service[Transactor[Task]]
        _ <- CredentialDefinitionSql.deleteAll.transact(tx)

        generatedCredentialDefinitions <- Generators.credentialDefinitionUnique.runCollectN(10)

        allCredentialDefinitionsHaveUniqueId = assert(
          generatedCredentialDefinitions
            .map(_.id)
            .toSet
            .count(_ => true)
        )(equalTo(generatedCredentialDefinitions.length))

        allCredentialDefinitionsHaveUniqueConstraint = assert(
          generatedCredentialDefinitions
            .map(_.uniqueConstraintKey)
            .toSet
            .count(_ => true)
        )(equalTo(generatedCredentialDefinitions.length))

        _ <- ZIO.collectAll(
          generatedCredentialDefinitions.map(credentialDefinition =>
            CredentialDefinitionSql.insert(credentialDefinition).transact(tx)
          )
        )

        firstActual = generatedCredentialDefinitions.head
        firstExpected <- CredentialDefinitionSql
          .findByGUID(firstActual.guid)
          .transact(tx)
          .map(_.headOption)

        credentialDefinitionCreated = assert(firstActual)(equalTo(firstExpected.get))

        totalCount <- CredentialDefinitionSql.totalCount.transact(tx)
        lookupCount <- CredentialDefinitionSql.lookupCount().transact(tx)

        totalCountIsN = assert(totalCount)(equalTo(generatedCredentialDefinitions.length))
        lookupCountIsN = assert(lookupCount)(equalTo(generatedCredentialDefinitions.length))

      } yield allCredentialDefinitionsHaveUniqueId &&
        allCredentialDefinitionsHaveUniqueConstraint &&
        credentialDefinitionCreated &&
        totalCountIsN && lookupCountIsN
    }
  ) @@ nondeterministic @@ sequential @@ timed
}
