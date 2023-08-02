package io.iohk.atala.pollux.core.model.schema.`type`

import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.schema.Schema
import zio.IO

trait CredentialSchemaType {
  val `type`: String
  def validate(schema: Schema): IO[CredentialSchemaError, Unit]
}
