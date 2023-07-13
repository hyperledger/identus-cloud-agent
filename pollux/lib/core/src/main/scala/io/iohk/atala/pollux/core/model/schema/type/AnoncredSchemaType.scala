package io.iohk.atala.pollux.core.model.schema.`type`

import com.networknt.schema.*
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.schema.Schema
import io.iohk.atala.pollux.core.model.schema.`type`.anoncred.{AnoncredSchemaSchemaV1, AnoncredSchemaSchemaVersion}
import io.iohk.atala.pollux.core.model.schema.common.JsonSchemaUtils
import io.iohk.atala.pollux.core.model.schema.validator.CredentialJsonSchemaValidator
import zio.*
import zio.json.*

object AnoncredSchemaType extends CredentialSchemaType {

  private val anoncredSchemaSchemaVersion: AnoncredSchemaSchemaVersion = AnoncredSchemaSchemaV1
  val `type`: String = AnoncredSchemaSchemaV1.version

  override def validate(schema: Schema): IO[CredentialSchemaError, Unit] = {
    for {
      jsonSchemaSchema <- anoncredSchemaSchemaVersion.initialiseJsonSchema
      schemaValidator = CredentialJsonSchemaValidator(jsonSchemaSchema)
      jsonSchemaNode <- JsonSchemaUtils.toJsonNode(schema)
      _ <- schemaValidator.validate(jsonSchemaNode)
    } yield ()
  }
}
