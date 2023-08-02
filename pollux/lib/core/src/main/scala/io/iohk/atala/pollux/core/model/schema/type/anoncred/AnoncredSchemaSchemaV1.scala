package io.iohk.atala.pollux.core.model.schema.`type`.anoncred

import com.networknt.schema.*
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.schema.common.JsonSchemaUtils
import zio.*

object AnoncredSchemaSchemaV1 extends AnoncredSchemaSchemaVersion {
  val version: String = AnoncredSchemaSchemaV1.getClass.getSimpleName
  private val jsonSchemaSchemaStr: String =
    """
      |{
      |  "$schema": "http://json-schema.org/draft-07/schema#",
      |  "type": "object",
      |  "properties": {
      |    "name": {
      |      "type": "string",
      |       "minLength": 1
      |    },
      |    "version": {
      |      "type": "string",
      |       "minLength": 1
      |    },
      |    "attrNames": {
      |      "type": "array",
      |      "items": {
      |        "type": "string",
      |         "minLength": 1
      |      },
      |      "minItems": 1,
      |      "maxItems": 125,
      |      "uniqueItems": true
      |    },
      |    "issuerId": {
      |      "type": "string",
      |      "minLength": 1
      |    }
      |  },
      |  "required": ["name", "version", "attrNames", "issuerId"]
      |}
      |""".stripMargin

  override def initialiseJsonSchema: IO[CredentialSchemaError, JsonSchema] =
    JsonSchemaUtils.jsonSchema(jsonSchemaSchemaStr)
}
