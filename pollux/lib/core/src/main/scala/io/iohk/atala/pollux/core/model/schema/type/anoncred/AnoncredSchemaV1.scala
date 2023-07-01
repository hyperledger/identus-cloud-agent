package io.iohk.atala.pollux.core.model.schema.`type`.anoncred

import com.networknt.schema.*
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.schema.common.JsonSchemaUtils
import zio.*

object AnoncredSchemaV1 extends AnoncredSchemaVersion {
  private val jsonSchemaSchemaStr: String =
    """
      |{
      |  "$schema": "http://json-schema.org/draft-07/schema#",
      |  "type": "object",
      |  "properties": {
      |    "name": {
      |      "type": "string"
      |    },
      |    "version": {
      |      "type": "string"
      |    },
      |    "attrNames": {
      |      "type": "array",
      |      "items": {
      |        "type": "string"
      |      },
      |      "minItems": 1,
      |      "maxItems": 125,
      |      "uniqueItems": true
      |    },
      |    "issuerId": {
      |      "type": "string"
      |    }
      |  },
      |  "required": ["name", "version", "attrNames", "issuerId"]
      |}
      |""".stripMargin

  override def initialiseJsonSchema: IO[CredentialSchemaError, JsonSchema] = JsonSchemaUtils.jsonSchema(jsonSchemaSchemaStr)
}
