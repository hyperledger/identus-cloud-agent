package org.hyperledger.identus.mercury.protocol.presentproof

import io.circe._
import io.circe.generic.semiauto._
import org.hyperledger.identus.mercury.model.DidId

case class ProofType(
    schema: String, // Schema ID EX: https://schema.org/Person
    requiredFields: Option[Seq[String]], // ["email"]
    trustIssuers: Option[Seq[DidId]] // ["did:prism:123321123"]
)
object ProofType {
  given Encoder[ProofType] = deriveEncoder[ProofType]
  given Decoder[ProofType] = deriveDecoder[ProofType]
}
