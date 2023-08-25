package io.iohk.atala.pollux.core.model.schema.`type`

import io.iohk.atala.pollux.core.model.schema.Schema
import io.iohk.atala.pollux.core.model.schema.validator.JsonSchemaError
import zio.IO

trait CredentialSchemaType {
  val `type`: String

  def validate(schema: Schema): IO[JsonSchemaError, Unit]
}
