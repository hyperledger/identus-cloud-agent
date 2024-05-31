package org.hyperledger.identus.pollux.core.model

import io.circe._
import io.circe.generic.semiauto._
import org.hyperledger.identus.pollux.core.model.presentation.{Options, PresentationDefinition}

final case class CredentialOfferAttachment(options: Options, presentation_definition: PresentationDefinition)

object CredentialOfferAttachment {
  given Encoder[CredentialOfferAttachment] = deriveEncoder
  given Decoder[CredentialOfferAttachment] = deriveDecoder
}
