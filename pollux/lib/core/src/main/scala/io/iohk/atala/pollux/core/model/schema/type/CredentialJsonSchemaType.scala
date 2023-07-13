package io.iohk.atala.pollux.core.model.schema.`type`

import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.schema.Schema
import io.iohk.atala.pollux.core.model.schema.validator.CredentialJsonSchemaValidator
import zio.*
import zio.json.*

object CredentialJsonSchemaType extends CredentialSchemaType {
  val VC_JSON_SCHEMA_URI = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json"

  override val `type`: String = VC_JSON_SCHEMA_URI

  override def validate(schema: Schema): IO[CredentialSchemaError, Unit] =
    for {
      _ <- CredentialJsonSchemaValidator.from(schema)
    } yield ()
}
