package io.iohk.atala.pollux.core.model.schema.`type`.anoncred

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.{JsonSchema, JsonSchemaFactory, SpecVersionDetector}
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError.{JsonSchemaParsingError, UnexpectedError}
import io.iohk.atala.pollux.core.model.schema.`type`.anoncred.AnoncredSchemaV1.jsonSchemaSchemaStr
import zio.{IO, ZIO}

trait AnoncredSchemaVersion {

  def initialiseJsonSchema: IO[CredentialSchemaError, JsonSchema]
}
