package io.iohk.atala.pollux.core.model

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

// FIXME: Fix the format according to some standard / RFC (ATL-3507)
// This is likely wrong and not conform to the standard.
// Until the format / flow is agreed, We need a workaround to make the issuing flow work.
// Here is just a temporary way to attach subjectId when sending CredentialOffer to the holder
final case class CredentialOfferAttachment(subjectId: String)

object CredentialOfferAttachment {
  given Encoder[CredentialOfferAttachment] = deriveEncoder
  given Decoder[CredentialOfferAttachment] = deriveDecoder
}
