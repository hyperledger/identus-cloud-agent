package io.iohk.atala.pollux.core.service.serdes.credentialdefinition

import io.iohk.atala.pollux.core.model.schema.validator.SchemaSerDes
import io.iohk.atala.pollux.core.service.serdes.credentialdefinition.ProofKeyV1
import zio.*
import zio.json.*

case class ProofKeyV1(c: String, xz_cap: String, xr_cap: List[List[String]])

object ProofKeyV1 {
  val version: String = "ProofKeyCredentialDefinitionV1"
  private val schema: String =
    """
      |{
      |  "$schema": "http://json-schema.org/draft-07/schema#",
      |  "type": "object",
      |  "properties": {
      |    "c": {
      |      "type": "string"
      |    },
      |    "xz_cap": {
      |      "type": "string"
      |    },
      |    "xr_cap": {
      |      "type": "array",
      |      "items": {
      |        "type": "array",
      |        "items": {
      |          "type": "string"
      |        }
      |      }
      |    }
      |  },
      |  "required": [
      |    "c",
      |    "xz_cap",
      |    "xr_cap"
      |  ]
      |}
      |
      |""".stripMargin

  val schemaSerDes: SchemaSerDes[ProofKeyV1] = SchemaSerDes(schema)

  given JsonDecoder[ProofKeyV1] =
    DeriveJsonDecoder.gen[ProofKeyV1]

  given JsonEncoder[ProofKeyV1] =
    DeriveJsonEncoder.gen[ProofKeyV1]
}
