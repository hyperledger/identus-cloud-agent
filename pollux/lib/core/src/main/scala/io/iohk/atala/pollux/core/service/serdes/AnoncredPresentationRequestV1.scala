package io.iohk.atala.pollux.core.service.serdes

import io.iohk.atala.pollux.core.model.schema.validator.SchemaSerDes
import zio.*
import zio.json.*

case class AnoncredPresentationRequestV1(
    requested_attributes: Map[String, AnoncredRequestedAttributeV1],
    requested_predicates: Map[String, AnoncredRequestedPredicateV1],
    name: String,
    nonce: String,
    version: String,
    non_revoked: Option[AnoncredNonRevokedIntervalV1]
)

case class AnoncredRequestedAttributeV1(name: String, restrictions: List[AnoncredAttributeRestrictionV1])

case class AnoncredRequestedPredicateV1(
    name: String,
    p_type: String,
    p_value: Int,
    restrictions: List[AnoncredPredicateRestrictionV1]
)

case class AnoncredAttributeRestrictionV1(
    schema_id: Option[String],
    cred_def_id: Option[String],
    non_revoked: Option[AnoncredNonRevokedIntervalV1]
)

case class AnoncredPredicateRestrictionV1(
    schema_id: Option[String],
    cred_def_id: Option[String],
    non_revoked: Option[AnoncredNonRevokedIntervalV1]
)

case class AnoncredNonRevokedIntervalV1(from: Option[Int], to: Option[Int])

object AnoncredPresentationRequestV1 {
  val version: String = "PresentationRequestV1"

  private val schema: String =
    """
      |{
      |  "$schema": "http://json-schema.org/draft-07/schema#",
      |  "type": "object",
      |  "properties": {
      |    "requested_attributes": {
      |      "type": "object",
      |      "additionalProperties": {
      |        "type": "object",
      |        "properties": {
      |          "name": { "type": "string" },
      |          "restrictions": {
      |            "type": "array",
      |            "items": {
      |              "type": "object",
      |              "properties": {
      |                "schema_id": { "type": "string" },
      |                "cred_def_id": { "type": "string" },
      |                "non_revoked": {
      |                  "type": "object",
      |                  "properties": {
      |                    "from": { "type": "integer" },
      |                    "to": { "type": "integer" }
      |                  }
      |                }
      |              }
      |            }
      |          }
      |        },
      |        "required": ["name", "restrictions"]
      |      }
      |    },
      |    "requested_predicates": {
      |      "type": "object",
      |      "additionalProperties": {
      |        "type": "object",
      |        "properties": {
      |          "name": { "type": "string" },
      |          "p_type": { "type": "string" },
      |          "p_value": { "type": "integer" },
      |          "restrictions": {
      |            "type": "array",
      |            "items": {
      |              "type": "object",
      |              "properties": {
      |                "schema_id": { "type": "string" },
      |                "cred_def_id": { "type": "string" },
      |                "non_revoked": {
      |                  "type": "object",
      |                  "properties": {
      |                    "from": { "type": "integer" },
      |                    "to": { "type": "integer" }
      |                  }
      |                }
      |              }
      |            }
      |          }
      |        },
      |        "required": ["name", "p_type", "p_value", "restrictions"]
      |      }
      |    },
      |    "name": { "type": "string" },
      |    "nonce": { "type": "string" },
      |    "version": { "type": "string" },
      |    "non_revoked": {
      |      "type": "object",
      |      "properties": {
      |        "from": { "type": "integer" },
      |        "to": { "type": "integer" }
      |      }
      |    }
      |  },
      |  "required": ["requested_attributes", "requested_predicates", "name", "nonce", "version" ]
      |}
      |
      |""".stripMargin

  val schemaSerDes: SchemaSerDes[AnoncredPresentationRequestV1] = SchemaSerDes(schema)

  given JsonDecoder[AnoncredRequestedAttributeV1] =
    DeriveJsonDecoder.gen[AnoncredRequestedAttributeV1]

  given JsonEncoder[AnoncredRequestedAttributeV1] =
    DeriveJsonEncoder.gen[AnoncredRequestedAttributeV1]

  given JsonDecoder[AnoncredRequestedPredicateV1] =
    DeriveJsonDecoder.gen[AnoncredRequestedPredicateV1]

  given JsonEncoder[AnoncredRequestedPredicateV1] =
    DeriveJsonEncoder.gen[AnoncredRequestedPredicateV1]

  given JsonDecoder[AnoncredAttributeRestrictionV1] =
    DeriveJsonDecoder.gen[AnoncredAttributeRestrictionV1]

  given JsonEncoder[AnoncredNonRevokedIntervalV1] =
    DeriveJsonEncoder.gen[AnoncredNonRevokedIntervalV1]

  given JsonDecoder[AnoncredNonRevokedIntervalV1] =
    DeriveJsonDecoder.gen[AnoncredNonRevokedIntervalV1]

  given JsonEncoder[AnoncredAttributeRestrictionV1] =
    DeriveJsonEncoder.gen[AnoncredAttributeRestrictionV1]

  given JsonDecoder[AnoncredPredicateRestrictionV1] =
    DeriveJsonDecoder.gen[AnoncredPredicateRestrictionV1]

  given JsonEncoder[AnoncredPredicateRestrictionV1] =
    DeriveJsonEncoder.gen[AnoncredPredicateRestrictionV1]

  given JsonDecoder[AnoncredPresentationRequestV1] =
    DeriveJsonDecoder.gen[AnoncredPresentationRequestV1]

  given JsonEncoder[AnoncredPresentationRequestV1] =
    DeriveJsonEncoder.gen[AnoncredPresentationRequestV1]

}
