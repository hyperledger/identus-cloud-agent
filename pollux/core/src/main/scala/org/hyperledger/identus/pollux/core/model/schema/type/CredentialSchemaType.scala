package org.hyperledger.identus.pollux.core.model.schema.`type`

import org.hyperledger.identus.pollux.core.model.schema.Schema
import org.hyperledger.identus.pollux.core.model.schema.validator.JsonSchemaError
import zio.IO

trait CredentialSchemaType {
  val `type`: String

  def validate(schema: Schema): IO[JsonSchemaError, Unit]
}
