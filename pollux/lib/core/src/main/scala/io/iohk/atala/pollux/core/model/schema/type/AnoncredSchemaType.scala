package io.iohk.atala.pollux.core.model.schema.`type`

import com.networknt.schema.*
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.schema.Schema
import io.iohk.atala.pollux.core.model.schema.`type`.anoncred.{AnoncredSchemaV1, AnoncredSchemaVersion}
import io.iohk.atala.pollux.core.model.schema.common.JsonSchemaUtils
import io.iohk.atala.pollux.core.model.schema.validator.CredentialJsonSchemaValidator
import zio.*
import zio.json.*

object AnoncredSchemaType extends CredentialSchemaType {
  val `type`: String = "Anoncred"
  private val anoncredSchemaVersion: AnoncredSchemaVersion = AnoncredSchemaV1

  override def validate(schema: Schema): IO[CredentialSchemaError, Unit] = {
    for {
      jsonSchemaSchema <- anoncredSchemaVersion.initialiseJsonSchema
      schemaValidator = CredentialJsonSchemaValidator(jsonSchemaSchema)
      jsonSchemaNode <- JsonSchemaUtils.toJsonNode(schema)
      _ <- schemaValidator.validate(jsonSchemaNode)
    } yield ()
  }
}
