package org.hyperledger.identus.pollux.core.model.presentation

import io.circe.*
import io.circe.generic.semiauto.*
import org.hyperledger.identus.pollux.prex.PresentationDefinition

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
