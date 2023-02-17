package io.iohk.atala.pollux.core.model.presentation

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

case class Field(
    id: Option[String] = None,
    path: Seq[String] = Seq.empty,
    name: Option[String] = None,
    purpose: Option[String] = None
)
object Field {
  given Encoder[Field] = deriveEncoder[Field]
  given Decoder[Field] = deriveDecoder[Field]
}

case class Jwt(alg: Seq[String], proof_type: Seq[String])
object Jwt {
  given Encoder[Jwt] = deriveEncoder[Jwt]
  given Decoder[Jwt] = deriveDecoder[Jwt]
}
case class ClaimFormat(jwt: Jwt)
object ClaimFormat {
  given Encoder[ClaimFormat] = deriveEncoder[ClaimFormat]
  given Decoder[ClaimFormat] = deriveDecoder[ClaimFormat]
}
case class Constraints(fields: Option[Seq[Field]])
object Constraints {
  given Encoder[Constraints] = deriveEncoder[Constraints]
  given Decoder[Constraints] = deriveDecoder[Constraints]
}

/** Refer to <a href="https://identity.foundation/presentation-exchange/#input-descriptor">Input Descriptors</a>
  */
case class InputDescriptor(
    id: String = java.util.UUID.randomUUID.toString(),
    name: Option[String] = None,
    purpose: Option[String] = None,
    format: Option[ClaimFormat] = None,
    constraints: Constraints
)
object InputDescriptor {
  given Encoder[InputDescriptor] = deriveEncoder[InputDescriptor]
  given Decoder[InputDescriptor] = deriveDecoder[InputDescriptor]
}

/** Refer to <a href="https://identity.foundation/presentation-exchange/#presentation-definition">Presentation
  * Definition</a>
  */
case class PresentationDefinition(
    id: String = java.util.UUID.randomUUID.toString(), // UUID
    input_descriptors: Seq[InputDescriptor] = Seq.empty,
    name: Option[String] = None,
    purpose: Option[String] = None,
    format: Option[ClaimFormat] = None
)
object PresentationDefinition {
  given Encoder[PresentationDefinition] = deriveEncoder[PresentationDefinition]
  given Decoder[PresentationDefinition] = deriveDecoder[PresentationDefinition]
}

case class Options(challenge: String, domain: String)
object Options {
  given Encoder[Options] = deriveEncoder[Options]
  given Decoder[Options] = deriveDecoder[Options]
}

case class PresentationAttachment(options: Option[Options] = None, presentation_definition: PresentationDefinition)
object PresentationAttachment {
  given Encoder[PresentationAttachment] = deriveEncoder[PresentationAttachment]
  given Decoder[PresentationAttachment] = deriveDecoder[PresentationAttachment]

  def build(options: Option[Options] = None): PresentationAttachment = {
    val presentationDefinition =
      PresentationDefinition(input_descriptors = Seq.empty)
    PresentationAttachment(options, presentationDefinition)
  }
}
