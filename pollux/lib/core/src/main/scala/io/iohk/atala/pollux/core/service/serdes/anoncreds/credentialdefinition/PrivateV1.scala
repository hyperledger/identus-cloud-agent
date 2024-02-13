package io.iohk.atala.pollux.core.service.serdes.anoncreds.credentialdefinition

import io.iohk.atala.pollux.core.model.schema.validator.SchemaSerDes
import zio.*
import zio.json.*

case class PrimaryPrivateKeyV1(p: String, q: String)

case class RevocationPrivateKeyV1(x: String, sk: String)

case class PrivateValueV1(
    p_key: PrimaryPrivateKeyV1,
    r_key: Option[RevocationPrivateKeyV1]
)

case class PrivateV1(value: PrivateValueV1)

object PrivateV1 {
  val version: String = "PrivateCredentialDefinitionV1"
  private val schema: String =
    """
      |{
      |  "$schema": "http://json-schema.org/draft-07/schema#",
      |  "type": "object",
      |  "properties": {
      |    "value": {
      |      "type": "object",
      |      "properties": {
      |        "p_key": {
      |          "type": "object",
      |          "properties": {
      |            "p": { "type": "string" },
      |            "q": { "type": "string" }
      |          },
      |          "required": ["p", "q"]
      |        },
      |        "r_key": {
      |          "type": ["object", "null"],
      |          "properties": {
      |            "x": { "type": "string" },
      |            "sk": { "type": "string" }
      |          },
      |          "required": ["x", "sk"]
      |        }
      |      },
      |      "required": ["p_key"]
      |    }
      |  },
      |  "required": ["value"]
      |}
      |
      |""".stripMargin

  val schemaSerDes: SchemaSerDes[PrivateV1] = SchemaSerDes(schema)

  given JsonEncoder[PrimaryPrivateKeyV1] =
    DeriveJsonEncoder.gen[PrimaryPrivateKeyV1]

  given JsonDecoder[PrimaryPrivateKeyV1] =
    DeriveJsonDecoder.gen[PrimaryPrivateKeyV1]

  given JsonEncoder[RevocationPrivateKeyV1] =
    DeriveJsonEncoder.gen[RevocationPrivateKeyV1]

  given JsonDecoder[RevocationPrivateKeyV1] =
    DeriveJsonDecoder.gen[RevocationPrivateKeyV1]

  given JsonEncoder[PrivateValueV1] = DeriveJsonEncoder.gen[PrivateValueV1]

  given JsonDecoder[PrivateValueV1] = DeriveJsonDecoder.gen[PrivateValueV1]

  given JsonEncoder[PrivateV1] =
    DeriveJsonEncoder.gen[PrivateV1]

  given JsonDecoder[PrivateV1] =
    DeriveJsonDecoder.gen[PrivateV1]
}
