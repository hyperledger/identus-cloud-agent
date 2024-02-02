package io.iohk.atala.pollux.core.service.serdes.anoncreds

import io.iohk.atala.pollux.core.model.schema.validator.SchemaSerDes
import zio.*
import zio.json.*

case class PresentationRequestV1(
    requested_attributes: Map[String, RequestedAttributeV1],
    requested_predicates: Map[String, RequestedPredicateV1],
    name: String,
    nonce: String,
    version: String,
    non_revoked: Option[NonRevokedIntervalV1]
)

case class RequestedAttributeV1(
    name: String,
    restrictions: List[Map[String, String]],
    non_revoked: Option[NonRevokedIntervalV1]
)

case class RequestedPredicateV1(
    name: String,
    p_type: String,
    p_value: Int,
    restrictions: List[Map[String, String]],
    non_revoked: Option[NonRevokedIntervalV1]
)

case class NonRevokedIntervalV1(from: Option[Int], to: Option[Int])

object PresentationRequestV1 {
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
      |              "type": "object"
      |            }
      |          },
      |         "non_revoked": {
      |            "type": "object",
      |             "properties": {
      |               "from": { "type": "integer" },
      |               "to": { "type": "integer" }
      |             }
      |         }
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
      |              "type": "object"
      |            }
      |          },
      |          "non_revoked": {
      |             "type": "object",
      |             "properties": {
      |                    "from": { "type": "integer" },
      |                    "to": { "type": "integer" }
      |              }
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

  val schemaSerDes: SchemaSerDes[PresentationRequestV1] = SchemaSerDes(schema)

  given JsonDecoder[RequestedAttributeV1] =
    DeriveJsonDecoder.gen[RequestedAttributeV1]

  given JsonEncoder[RequestedAttributeV1] =
    DeriveJsonEncoder.gen[RequestedAttributeV1]

  given JsonDecoder[RequestedPredicateV1] =
    DeriveJsonDecoder.gen[RequestedPredicateV1]

  given JsonEncoder[RequestedPredicateV1] =
    DeriveJsonEncoder.gen[RequestedPredicateV1]

  given JsonEncoder[NonRevokedIntervalV1] =
    DeriveJsonEncoder.gen[NonRevokedIntervalV1]

  given JsonDecoder[NonRevokedIntervalV1] =
    DeriveJsonDecoder.gen[NonRevokedIntervalV1]

  given JsonDecoder[PresentationRequestV1] =
    DeriveJsonDecoder.gen[PresentationRequestV1]

  given JsonEncoder[PresentationRequestV1] =
    DeriveJsonEncoder.gen[PresentationRequestV1]

}
