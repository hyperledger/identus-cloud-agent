package org.hyperledger.identus.pollux.core.model

import io.circe.*
import io.circe.generic.semiauto.*
import org.hyperledger.identus.pollux.core.model.presentation.Options
import org.hyperledger.identus.pollux.prex.PresentationDefinition

final case class CredentialOfferAttachment(options: Options, presentation_definition: PresentationDefinition)

object CredentialOfferAttachment {
  given Encoder[CredentialOfferAttachment] = deriveEncoder
  given Decoder[CredentialOfferAttachment] = deriveDecoder
}
