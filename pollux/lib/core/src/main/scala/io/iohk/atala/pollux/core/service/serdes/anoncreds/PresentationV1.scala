package io.iohk.atala.pollux.core.service.serdes.anoncreds

import io.iohk.atala.pollux.core.model.schema.validator.SchemaSerDes
import io.iohk.atala.pollux.core.service.serdes.*
import zio.*
import zio.json.*

case class AdditionalPropertiesTypeStringV1(value: String)

case class RevealedAttrV1(sub_proof_index: Int, raw: String, encoded: String)

case class RequestedProofV1(
    revealed_attrs: Map[String, RevealedAttrV1],
    self_attested_attrs: Map[String, String],
    unrevealed_attrs: Map[String, String],
    predicates: Map[String, SubProofIndexV1]
)

case class SubProofIndexV1(sub_proof_index: Int)

case class IdentifierV1(
    schema_id: String,
    cred_def_id: String,
    rev_reg_id: Option[String],
    timestamp: Option[Int]
)

// Adjusted classes for proofs
case class EqProofV1(
    revealed_attrs: Map[String, String],
    a_prime: String,
    e: String,
    v: String,
    m: Map[String, String],
    m2: String
)

case class PredicateV1(attr_name: String, p_type: String, value: Int)

case class GeProofV1(
    u: Map[String, String],
    r: Map[String, String],
    mj: String,
    alpha: String,
    t: Map[String, String],
    predicate: PredicateV1
)

case class PrimaryProofV1(eq_proof: EqProofV1, ge_proofs: List[GeProofV1])

case class SubProofV1(primary_proof: PrimaryProofV1, non_revoc_proof: Option[NonRevocProofV1])

case class ProofV1(proofs: List[SubProofV1], aggregated_proof: AggregatedProofV1)

case class AggregatedProofV1(c_hash: String, c_list: List[List[Int]])

case class NonRevocProofXListV1(
    rho: String,
    r: String,
    r_prime: String,
    r_prime_prime: String,
    r_prime_prime_prime: String,
    o: String,
    o_prime: String,
    m: String,
    m_prime: String,
    t: String,
    t_prime: String,
    m2: String,
    s: String,
    c: String
)

case class NonRevocProofCListV1(e: String, d: String, a: String, g: String, w: String, s: String, u: String)

case class NonRevocProofV1(x_list: NonRevocProofXListV1, c_list: NonRevocProofCListV1)

case class PresentationV1(
    proof: ProofV1,
    requested_proof: RequestedProofV1,
    identifiers: List[IdentifierV1]
)

object PresentationV1 {
  val version: String = "PresentationV1"

  private val schema: String =
    """
      |{
      |   "$schema":"http://json-schema.org/draft-07/schema#",
      |   "type":"object",
      |   "properties":{
      |      "proof":{
      |         "type":"object",
      |         "properties":{
      |            "proofs":{
      |               "type":"array",
      |               "items":{
      |                  "type":"object",
      |                  "properties":{
      |                     "primary_proof":{
      |                        "type":"object",
      |                        "properties":{
      |                           "eq_proof":{
      |                              "type":"object",
      |                              "properties":{
      |                                 "revealed_attrs":{
      |                                    "type":"object",
      |                                    "additionalProperties":{
      |                                       "type":"string"
      |                                    }
      |                                 },
      |                                 "a_prime":{
      |                                    "type":"string"
      |                                 },
      |                                 "e":{
      |                                    "type":"string"
      |                                 },
      |                                 "v":{
      |                                    "type":"string"
      |                                 },
      |                                 "m":{
      |                                    "type":"object",
      |                                    "additionalProperties":{
      |                                       "type":"string"
      |                                    }
      |                                 },
      |                                 "m2":{
      |                                    "type":"string"
      |                                 }
      |                              },
      |                              "required":[
      |                                 "revealed_attrs",
      |                                 "a_prime",
      |                                 "e",
      |                                 "v",
      |                                 "m",
      |                                 "m2"
      |                              ]
      |                           },
      |                           "ge_proofs":{
      |                              "type":"array",
      |                              "items":{
      |                                 "type":"object",
      |                                 "properties":{
      |                                    "u":{
      |                                       "type":"object",
      |                                       "additionalProperties":{
      |                                          "type":"string"
      |                                       }
      |                                    },
      |                                    "r":{
      |                                       "type":"object",
      |                                       "additionalProperties":{
      |                                          "type":"string"
      |                                       }
      |                                    },
      |                                    "mj":{
      |                                       "type":"string"
      |                                    },
      |                                    "alpha":{
      |                                       "type":"string"
      |                                    },
      |                                    "t":{
      |                                       "type":"object",
      |                                       "additionalProperties":{
      |                                          "type":"string"
      |                                       }
      |                                    },
      |                                    "predicate":{
      |                                       "type":"object",
      |                                       "properties":{
      |                                          "attr_name":{
      |                                             "type":"string"
      |                                          },
      |                                          "p_type":{
      |                                             "type":"string"
      |                                          },
      |                                          "value":{
      |                                             "type":"integer"
      |                                          }
      |                                       },
      |                                       "required":[
      |                                          "attr_name",
      |                                          "p_type",
      |                                          "value"
      |                                       ]
      |                                    }
      |                                 },
      |                                 "required":[
      |                                    "u",
      |                                    "r",
      |                                    "mj",
      |                                    "alpha",
      |                                    "t",
      |                                    "predicate"
      |                                 ]
      |                              }
      |                           }
      |                        },
      |                        "required":[
      |                           "eq_proof",
      |                           "ge_proofs"
      |                        ]
      |                     },
      |                     "non_revoc_proof":{
      |                        "oneOf":[
      |                           {
      |                              "type":"object",
      |                              "properties":{
      |                                 "x_list":{
      |                                    "type":"object",
      |                                    "properties":{
      |                                       "rho":{
      |                                          "type":"string"
      |                                       },
      |                                       "r":{
      |                                          "type":"string"
      |                                       },
      |                                       "r_prime":{
      |                                          "type":"string"
      |                                       },
      |                                       "r_prime_prime":{
      |                                          "type":"string"
      |                                       },
      |                                       "r_prime_prime_prime":{
      |                                          "type":"string"
      |                                       },
      |                                       "o":{
      |                                          "type":"string"
      |                                       },
      |                                       "o_prime":{
      |                                          "type":"string"
      |                                       },
      |                                       "m":{
      |                                          "type":"string"
      |                                       },
      |                                       "m_prime":{
      |                                          "type":"string"
      |                                       },
      |                                       "t":{
      |                                          "type":"string"
      |                                       },
      |                                       "t_prime":{
      |                                          "type":"string"
      |                                       },
      |                                       "m2":{
      |                                          "type":"string"
      |                                       },
      |                                       "s":{
      |                                          "type":"string"
      |                                       },
      |                                       "c":{
      |                                          "type":"string"
      |                                       }
      |                                    },
      |                                    "required":[
      |                                       "rho",
      |                                       "r",
      |                                       "r_prime",
      |                                       "r_prime_prime",
      |                                       "r_prime_prime_prime",
      |                                       "o",
      |                                       "o_prime",
      |                                       "m",
      |                                       "m_prime",
      |                                       "t",
      |                                       "t_prime",
      |                                       "m2",
      |                                       "s",
      |                                       "c"
      |                                    ]
      |                                 },
      |                                 "c_list":{
      |                                    "type":"object",
      |                                    "properties":{
      |                                       "e":{
      |                                          "type":"string"
      |                                       },
      |                                       "d":{
      |                                          "type":"string"
      |                                       },
      |                                       "a":{
      |                                          "type":"string"
      |                                       },
      |                                       "g":{
      |                                          "type":"string"
      |                                       },
      |                                       "w":{
      |                                          "type":"string"
      |                                       },
      |                                       "s":{
      |                                          "type":"string"
      |                                       },
      |                                       "u":{
      |                                          "type":"string"
      |                                       }
      |                                    },
      |                                    "required":[
      |                                       "e",
      |                                       "d",
      |                                       "a",
      |                                       "g",
      |                                       "w",
      |                                       "s",
      |                                       "u"
      |                                    ]
      |                                 }
      |                              },
      |                              "required":[
      |                                 "x_list",
      |                                 "c_list"
      |                              ]
      |                           },
      |                           {
      |                              "type":"null"
      |                           }
      |                        ]
      |                     }
      |                  },
      |                  "required":[
      |                     "primary_proof"
      |                  ]
      |               }
      |            },
      |            "aggregated_proof":{
      |               "type":"object",
      |               "properties":{
      |                  "c_hash":{
      |                     "type":"string"
      |                  },
      |                  "c_list":{
      |                     "type":"array",
      |                     "items":{
      |                        "type":"array",
      |                        "items":{
      |                           "type":"integer"
      |                        }
      |                     }
      |                  }
      |               },
      |               "required":[
      |                  "c_hash",
      |                  "c_list"
      |               ]
      |            }
      |         },
      |         "required":[
      |            "proofs",
      |            "aggregated_proof"
      |         ]
      |      },
      |      "requested_proof":{
      |         "type":"object",
      |         "properties":{
      |            "revealed_attrs":{
      |               "type":"object",
      |               "additionalProperties":{
      |                  "type":"object",
      |                  "properties":{
      |                     "sub_proof_index":{
      |                        "type":"integer"
      |                     },
      |                     "raw":{
      |                        "type":"string"
      |                     },
      |                     "encoded":{
      |                        "type":"string"
      |                     }
      |                  },
      |                  "required":[
      |                     "sub_proof_index",
      |                     "raw",
      |                     "encoded"
      |                  ]
      |               }
      |            },
      |            "self_attested_attrs":{
      |               "type":"object"
      |            },
      |            "unrevealed_attrs":{
      |               "type":"object"
      |            },
      |            "predicates":{
      |               "type":"object",
      |               "additionalProperties":{
      |                  "type":"object",
      |                  "properties":{
      |                     "sub_proof_index":{
      |                        "type":"integer"
      |                     }
      |                  },
      |                  "required":[
      |                     "sub_proof_index"
      |                  ]
      |               }
      |            }
      |         },
      |         "required":[
      |            "revealed_attrs",
      |            "self_attested_attrs",
      |            "unrevealed_attrs",
      |            "predicates"
      |         ]
      |      },
      |      "identifiers":{
      |         "type":"array",
      |         "items":{
      |            "type":"object",
      |            "properties":{
      |               "schema_id":{
      |                  "type":"string"
      |               },
      |               "cred_def_id":{
      |                  "type":"string"
      |               },
      |               "rev_reg_id":{
      |                  "type":[
      |                     "string",
      |                     "null"
      |                  ]
      |               },
      |               "timestamp":{
      |                  "type":[
      |                     "integer",
      |                     "null"
      |                  ]
      |               }
      |            },
      |            "required":[
      |               "schema_id",
      |               "cred_def_id"
      |            ]
      |         }
      |      }
      |   },
      |   "required":[
      |      "proof",
      |      "requested_proof",
      |      "identifiers"
      |   ]
      |}
      |""".stripMargin

  val schemaSerDes: SchemaSerDes[PresentationV1] = SchemaSerDes(schema)

  given JsonDecoder[AdditionalPropertiesTypeStringV1] =
    DeriveJsonDecoder.gen[AdditionalPropertiesTypeStringV1]

  given JsonEncoder[AdditionalPropertiesTypeStringV1] =
    DeriveJsonEncoder.gen[AdditionalPropertiesTypeStringV1]

  given JsonDecoder[EqProofV1] = DeriveJsonDecoder.gen[EqProofV1]

  given JsonEncoder[EqProofV1] = DeriveJsonEncoder.gen[EqProofV1]

  given JsonDecoder[PredicateV1] = DeriveJsonDecoder.gen[PredicateV1]

  given JsonEncoder[PredicateV1] = DeriveJsonEncoder.gen[PredicateV1]

  given JsonDecoder[GeProofV1] = DeriveJsonDecoder.gen[GeProofV1]

  given JsonEncoder[GeProofV1] = DeriveJsonEncoder.gen[GeProofV1]

  given JsonDecoder[NonRevocProofXListV1] = DeriveJsonDecoder.gen[NonRevocProofXListV1]

  given JsonEncoder[NonRevocProofXListV1] = DeriveJsonEncoder.gen[NonRevocProofXListV1]

  given JsonDecoder[NonRevocProofCListV1] = DeriveJsonDecoder.gen[NonRevocProofCListV1]

  given JsonEncoder[NonRevocProofCListV1] = DeriveJsonEncoder.gen[NonRevocProofCListV1]

  given JsonDecoder[NonRevocProofV1] = DeriveJsonDecoder.gen[NonRevocProofV1]

  given JsonEncoder[NonRevocProofV1] = DeriveJsonEncoder.gen[NonRevocProofV1]

  given JsonDecoder[PrimaryProofV1] = DeriveJsonDecoder.gen[PrimaryProofV1]

  given JsonEncoder[PrimaryProofV1] = DeriveJsonEncoder.gen[PrimaryProofV1]

  given JsonDecoder[AggregatedProofV1] = DeriveJsonDecoder.gen[AggregatedProofV1]

  given JsonEncoder[AggregatedProofV1] = DeriveJsonEncoder.gen[AggregatedProofV1]

  given JsonDecoder[SubProofV1] = DeriveJsonDecoder.gen[SubProofV1]

  given JsonEncoder[SubProofV1] = DeriveJsonEncoder.gen[SubProofV1]

  given JsonDecoder[ProofV1] = DeriveJsonDecoder.gen[ProofV1]

  given JsonEncoder[ProofV1] = DeriveJsonEncoder.gen[ProofV1]

  given JsonDecoder[RevealedAttrV1] = DeriveJsonDecoder.gen[RevealedAttrV1]

  given JsonEncoder[RevealedAttrV1] = DeriveJsonEncoder.gen[RevealedAttrV1]

  given JsonDecoder[SubProofIndexV1] = DeriveJsonDecoder.gen[SubProofIndexV1]

  given JsonEncoder[SubProofIndexV1] = DeriveJsonEncoder.gen[SubProofIndexV1]

  given JsonDecoder[RequestedProofV1] = DeriveJsonDecoder.gen[RequestedProofV1]

  given JsonEncoder[RequestedProofV1] = DeriveJsonEncoder.gen[RequestedProofV1]

  given JsonDecoder[IdentifierV1] = DeriveJsonDecoder.gen[IdentifierV1]

  given JsonEncoder[IdentifierV1] = DeriveJsonEncoder.gen[IdentifierV1]

  given JsonDecoder[PresentationV1] = DeriveJsonDecoder.gen[PresentationV1]

  given JsonEncoder[PresentationV1] = DeriveJsonEncoder.gen[PresentationV1]

}
