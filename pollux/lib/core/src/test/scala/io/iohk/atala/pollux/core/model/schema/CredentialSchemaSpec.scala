package io.iohk.atala.pollux.core.model.schema

import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.schema.AnoncredSchemaTypeSpec.test
import io.iohk.atala.pollux.core.model.schema.`type`.{AnoncredSchemaType, CredentialJsonSchemaType}
import zio.Scope
import zio.json.*
import zio.json.ast.Json
import zio.json.ast.Json.*
import zio.test.Assertion.*
import zio.test.{Assertion, Spec, TestEnvironment, ZIOSpecDefault, assertZIO}

import java.time.OffsetDateTime
import java.util.UUID

object CredentialSchemaSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = suite("CredentialSchemaTest")(
    suite("resolveCredentialSchemaType")(
      test("should return AnoncredSchemaType for a supported schema type") {

        val schemaType = AnoncredSchemaType.`type`
        val result = CredentialSchema.resolveCredentialSchemaType(schemaType)
        assertZIO(result)(Assertion.equalTo(AnoncredSchemaType))
      },
      test("should return CredentialJsonSchemaType for a supported schema type") {
        val schemaType = CredentialJsonSchemaType.`type`
        val result = CredentialSchema.resolveCredentialSchemaType(schemaType)
        assertZIO(result)(Assertion.equalTo(CredentialJsonSchemaType))
      },
      test("should fail with UnsupportedCredentialSchemaType for an unsupported schema type") {
        val schemaType = "UnsupportedSchemaType"
        val result = CredentialSchema.resolveCredentialSchemaType(schemaType)
        assertZIO(result.exit)(
          fails(
            isSubtype[CredentialSchemaError.UnsupportedCredentialSchemaType](
              hasField("userMessage", _.userMessage, equalTo(s"Unsupported VC Schema type $schemaType"))
            )
          )
        )
      }
    ),
    suite("validateCredentialSchema")(
      test("should validate a correct schema") {
        val jsonSchema =
          """
            |{
            |  "name": "Anoncred",
            |  "version": "1.0",
            |  "attrNames": ["attr1", "attr2"],
            |  "issuerId": "issuer"
            |}
            |""".stripMargin

        val credentialSchema = CredentialSchema(
          guid = UUID.randomUUID(),
          id = UUID.randomUUID(),
          name = "Anoncred",
          version = "1.0",
          author = "author",
          authored = OffsetDateTime.parse("2022-03-10T12:00:00Z"),
          tags = Seq("tag1", "tag2"),
          description = "Anoncred Schema",
          `type` = AnoncredSchemaType.`type`,
          schema = jsonSchema.fromJson[Json].getOrElse(Json.Null)
        )
        assertZIO(CredentialSchema.validateCredentialSchema(credentialSchema))(isUnit)
      },
      test("should fail for attrName not unique") {
        val jsonSchema =
          """
            |{
            |  "name": "Anoncred",
            |  "version": "1.0",
            |  "attrNames": ["attr1", "attr1"],
            |  "issuerId": "issuer"
            |}
            |""".stripMargin
        val credentialSchema = CredentialSchema(
          guid = UUID.randomUUID(),
          id = UUID.randomUUID(),
          name = "Anoncred",
          version = "1.0",
          author = "author",
          authored = OffsetDateTime.parse("2022-03-10T12:00:00Z"),
          tags = Seq("tag1", "tag2"),
          description = "Anoncred Schema",
          `type` = AnoncredSchemaType.`type`,
          schema = jsonSchema.fromJson[Json].getOrElse(Json.Null)
        )
        assertZIO(CredentialSchema.validateCredentialSchema(credentialSchema).exit)(
          AnoncredSchemaTypeSpec.failsWithErrors(
            Seq("$.attrNames: the items in the array must be unique")
          )
        )
      },
      test("should validate a correct schema") {
        val jsonSchema =
          """
            |{
            |  "name": "Anoncred",
            |  "version": "1.0",
            |  "attrNames": ["attr1", "attr2"],
            |  "issuerId": "issuer"
            |}
            |""".stripMargin
        val schemaType = "UnsupportedSchemaType"
        val credentialSchema = CredentialSchema(
          guid = UUID.randomUUID(),
          id = UUID.randomUUID(),
          name = "Anoncred",
          version = "1.0",
          author = "author",
          authored = OffsetDateTime.parse("2022-03-10T12:00:00Z"),
          tags = Seq("tag1", "tag2"),
          description = "Anoncred Schema",
          `type` = schemaType,
          schema = jsonSchema.fromJson[Json].getOrElse(Json.Null)
        )
        val result = CredentialSchema.validateCredentialSchema(credentialSchema)
        assertZIO(result.exit)(
          fails(
            isSubtype[CredentialSchemaError.UnsupportedCredentialSchemaType](
              hasField("userMessage", _.userMessage, equalTo(s"Unsupported VC Schema type $schemaType"))
            )
          )
        )
      }
    )
  )
}
