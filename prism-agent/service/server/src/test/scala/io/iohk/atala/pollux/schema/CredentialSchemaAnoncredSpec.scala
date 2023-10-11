package io.iohk.atala.pollux.schema

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.container.util.MigrationAspects.*
import io.iohk.atala.iam.authentication.AuthenticatorWithAuthZ
import io.iohk.atala.pollux.core.model.schema.`type`.anoncred.AnoncredSchemaSerDesV1
import io.iohk.atala.pollux.core.model.schema.`type`.{AnoncredSchemaType, CredentialJsonSchemaType}
import io.iohk.atala.pollux.credentialschema.*
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaController
import io.iohk.atala.pollux.credentialschema.http.{CredentialSchemaInput, CredentialSchemaResponse}
import sttp.client3.basicRequest
import sttp.client3.ziojson.{asJsonAlways, *}
import sttp.model.StatusCode
import zio.*
import zio.ZIO.*
import zio.json.*
import zio.json.ast.Json
import zio.json.ast.Json.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

import java.util.UUID

object CredentialSchemaAnoncredSpec extends ZIOSpecDefault with CredentialSchemaTestTools:
  private def createCredentialSchemaInput(schemaType: String) = {
    val jsonSchema =
      """
        |{
        |  "name": "Anoncred",
        |  "version": "1.0",
        |  "attrNames": ["attr1", "attr2"],
        |  "issuerId": "issuer"
        |}
        |""".stripMargin

    CredentialSchemaInput(
      name = "TestSchema",
      version = "1.0.0",
      description = Option("schema description"),
      `type` = schemaType,
      schema = jsonSchema.fromJson[Json].getOrElse(Json.Null),
      tags = List("test"),
      author = "did:prism:557a4ef2ed0cf86fb50d91577269136b3763722ef00a72a1fb1e66895f52b6d8"
    )
  }

  def spec =
    wrapSpec(schemaCreateAndGetOperationsSpec)
      + wrapSpec(unsupportedSchemaSpec)
      + wrapSpec(wrongSchemaSpec)

  private def wrapSpec(spec: Spec[CredentialSchemaController & AuthenticatorWithAuthZ[BaseEntity], Throwable]) = {
    (spec
      @@ nondeterministic @@ sequential @@ timed @@ migrateEach(
        schema = "public",
        paths = "classpath:sql/pollux"
      )).provideSomeLayerShared(
      mockManagedDIDServiceLayer.toLayer >+> testEnvironmentLayer
    )
  }

  private val schemaCreateAndGetOperationsSpec = {
    def getSchemaZIO(uuid: UUID) = for {
      controller <- ZIO.service[CredentialSchemaController]
      authenticator <- ZIO.service[AuthenticatorWithAuthZ[BaseEntity]]
      backend = httpBackend(controller, authenticator)
      response <- basicRequest
        .get(credentialSchemaUriBase.addPath(uuid.toString))
        .response(asJsonAlways[CredentialSchemaResponse])
        .send(backend)
      fetchedSchema <- fromEither(response.body)
    } yield fetchedSchema

    suite("Anoncred Schema Creation")(
      test("should create new Schema") {
        val schemaInput = createCredentialSchemaInput(AnoncredSchemaSerDesV1.version)
        for {
          response <- createResponse[CredentialSchemaResponse](AnoncredSchemaType.`type`)
          statusCodeIs201 = assert(response.code)(equalTo(StatusCode.Created))

          credentialSchema <- fromEither(response.body)
          actualFields = CredentialSchemaInput(
            name = credentialSchema.name,
            version = credentialSchema.version,
            description = Option(credentialSchema.description),
            `type` = credentialSchema.`type`,
            schema = credentialSchema.schema,
            tags = credentialSchema.tags,
            author = credentialSchema.author
          )

          credentialSchemaIsCreated = assert(schemaInput)(equalTo(actualFields))

          fetchedSchema <- getSchemaZIO(credentialSchema.guid)

          credentialSchemaIsFetched = assert(fetchedSchema)(equalTo(credentialSchema))

        } yield statusCodeIs201 && credentialSchemaIsCreated && credentialSchemaIsFetched
      }
    )
  }

  private val unsupportedSchemaSpec = {
    suite("Anoncred Schema Creation")(
      test("should fail given unsupported Schema Type") {
        for {
          response <- createResponse[ErrorResponse]("WrongSchema")
        } yield assert(response.body)(
          isRight(hasField("detail", _.detail, isSome(equalTo("Unsupported VC Schema type WrongSchema"))))
        )
      }
    )
  }

  private val wrongSchemaSpec = {
    suite("Anoncred Schema Creation")(
      test("should fail given wrong Schema Type") {
        for {
          response <- createResponse[ErrorResponse](CredentialJsonSchemaType.`type`)
        } yield assert(response.body)(
          isRight(
            hasField("detail", _.detail, isSome(equalTo("'$schema' tag is not present")))
          )
        )
      }
    )
  }

  private def createResponse[B: JsonDecoder](schemaType: String) = {
    for {
      controller <- ZIO.service[CredentialSchemaController]
      authenticator <- ZIO.service[AuthenticatorWithAuthZ[BaseEntity]]
      backend = httpBackend(controller, authenticator)
      response <-
        basicRequest
          .post(credentialSchemaUriBase)
          .body(createCredentialSchemaInput(schemaType).toJsonPretty)
          .response(asJsonAlways[B])
          .send(backend)
    } yield response
  }
