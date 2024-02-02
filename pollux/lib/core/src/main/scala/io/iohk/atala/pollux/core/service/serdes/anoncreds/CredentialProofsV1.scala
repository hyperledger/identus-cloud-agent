package io.iohk.atala.pollux.core.service.serdes.anoncreds

import io.iohk.atala.pollux.core.model.schema.validator.SchemaSerDes
import zio.*
import zio.json.*

case class CredentialProofV1(
    credential: String,
    requestedAttribute: Seq[String],
    requestedPredicate: Seq[String]
)

case class CredentialProofsV1(credentialProofs: List[CredentialProofV1])

object CredentialProofsV1 {
  val version: String = "AnoncredCredentialProofsV1"
  private val schema: String =
    """
      |{
      |  "$schema": "http://json-schema.org/draft-07/schema#",
      |  "type": "object",
      |  "properties": {
      |    "credentialProofs": {
      |      "type": "array",
      |      "items": {
      |        "type": "object",
      |        "properties": {
      |          "credential": {
      |            "type": "string"
      |          },
      |          "requestedAttribute": {
      |            "type": "array",
      |            "items": {
      |              "type": "string"
      |            }
      |          },
      |          "requestedPredicate": {
      |            "type": "array",
      |            "items": {
      |              "type": "string"
      |            }
      |          }
      |        },
      |        "required": ["credential", "requestedAttribute", "requestedPredicate"]
      |      }
      |    }
      |  },
      |  "required": ["credentialProofs"]
      |}
      |""".stripMargin

  val schemaSerDes: SchemaSerDes[CredentialProofsV1] = SchemaSerDes(schema)

  given JsonDecoder[CredentialProofV1] =
    DeriveJsonDecoder.gen[CredentialProofV1]

  given JsonEncoder[CredentialProofV1] =
    DeriveJsonEncoder.gen[CredentialProofV1]

  given JsonDecoder[CredentialProofsV1] =
    DeriveJsonDecoder.gen[CredentialProofsV1]

  given JsonEncoder[CredentialProofsV1] =
    DeriveJsonEncoder.gen[CredentialProofsV1]
}
