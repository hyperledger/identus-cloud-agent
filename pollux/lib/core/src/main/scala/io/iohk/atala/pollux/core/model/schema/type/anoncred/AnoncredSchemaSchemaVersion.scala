package io.iohk.atala.pollux.core.model.schema.`type`.anoncred

import com.networknt.schema.JsonSchema
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import zio.IO

trait AnoncredSchemaSchemaVersion {
  val version: String

  def initialiseJsonSchema: IO[CredentialSchemaError, JsonSchema]
}
