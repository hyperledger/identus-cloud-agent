package org.hyperledger.identus.pollux.core.model.presentation

import org.hyperledger.identus.pollux.sdjwt.PresentationJson
import zio.json.*

case class SdJwtPresentationPayload(
    claimsToDisclose: ast.Json.Obj,
    presentation: PresentationJson,
    options: Option[Options]
)
object SdJwtPresentationPayload {
  given JsonDecoder[Options] = DeriveJsonDecoder.gen[Options]
  given JsonEncoder[Options] = DeriveJsonEncoder.gen[Options]
  given JsonDecoder[SdJwtPresentationPayload] = DeriveJsonDecoder.gen[SdJwtPresentationPayload]
  given JsonEncoder[SdJwtPresentationPayload] = DeriveJsonEncoder.gen[SdJwtPresentationPayload]
}
