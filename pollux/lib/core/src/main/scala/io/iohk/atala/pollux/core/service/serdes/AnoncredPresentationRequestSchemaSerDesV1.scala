package io.iohk.atala.pollux.core.service.serdes

import io.iohk.atala.pollux.core.model.schema.validator.SchemaSerDes
import zio.*
import zio.json.*

case class AnoncredPresentationRequestSchemaSerDesV1(
    requested_attributes: Map[String, AnoncredRequestedAttribute],
    requested_predicates: Map[String, AnoncredRequestedPredicate],
    name: String,
    nonce: String,
    version: String,
    non_revoked: Option[AnoncredNonRevokedInterval]
)

case class AnoncredRequestedAttribute(name: String, restrictions: List[AnoncredAttributeRestriction])

case class AnoncredRequestedPredicate(
    name: String,
    p_type: String,
    p_value: Int,
    restrictions: List[AnoncredPredicateRestriction]
)

case class AnoncredAttributeRestriction(
    schema_id: Option[String],
    cred_def_id: Option[String],
    non_revoked: Option[AnoncredNonRevokedInterval]
)

case class AnoncredPredicateRestriction(
    schema_id: Option[String],
    cred_def_id: Option[String],
    non_revoked: Option[AnoncredNonRevokedInterval]
)

case class AnoncredNonRevokedInterval(from: Option[Int], to: Option[Int])

object AnoncredPresentationRequestSchemaSerDesV1 {
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

  val schemaSerDes: SchemaSerDes[AnoncredPresentationRequestSchemaSerDesV1] = SchemaSerDes(schema)

  given JsonDecoder[AnoncredRequestedAttribute] =
    DeriveJsonDecoder.gen[AnoncredRequestedAttribute]

  given JsonEncoder[AnoncredRequestedAttribute] =
    DeriveJsonEncoder.gen[AnoncredRequestedAttribute]

  given JsonDecoder[AnoncredRequestedPredicate] =
    DeriveJsonDecoder.gen[AnoncredRequestedPredicate]

  given JsonEncoder[AnoncredRequestedPredicate] =
    DeriveJsonEncoder.gen[AnoncredRequestedPredicate]

  given JsonDecoder[AnoncredAttributeRestriction] =
    DeriveJsonDecoder.gen[AnoncredAttributeRestriction]

  given JsonEncoder[AnoncredNonRevokedInterval] =
    DeriveJsonEncoder.gen[AnoncredNonRevokedInterval]

  given JsonDecoder[AnoncredNonRevokedInterval] =
    DeriveJsonDecoder.gen[AnoncredNonRevokedInterval]

  given JsonEncoder[AnoncredAttributeRestriction] =
    DeriveJsonEncoder.gen[AnoncredAttributeRestriction]

  given JsonDecoder[AnoncredPredicateRestriction] =
    DeriveJsonDecoder.gen[AnoncredPredicateRestriction]

  given JsonEncoder[AnoncredPredicateRestriction] =
    DeriveJsonEncoder.gen[AnoncredPredicateRestriction]

  given JsonDecoder[AnoncredPresentationRequestSchemaSerDesV1] =
    DeriveJsonDecoder.gen[AnoncredPresentationRequestSchemaSerDesV1]

  given JsonEncoder[AnoncredPresentationRequestSchemaSerDesV1] =
    DeriveJsonEncoder.gen[AnoncredPresentationRequestSchemaSerDesV1]

}
