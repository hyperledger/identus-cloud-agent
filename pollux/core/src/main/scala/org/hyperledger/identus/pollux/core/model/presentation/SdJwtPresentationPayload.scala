package org.hyperledger.identus.pollux.core.model.presentation

import org.hyperledger.identus.pollux.sdjwt.PresentationCompact
import zio.json.*

//FIXME
type SdJwtPresentationPayload = PresentationCompact
// case class SdJwtPresentationPayload(
//     claimsToDisclose: ast.Json.Obj,
//     presentation: PresentationCompact,
//     options: Option[Options]
// )
// object SdJwtPresentationPayload {
//   given JsonDecoder[Options] = DeriveJsonDecoder.gen[Options]
//   given JsonEncoder[Options] = DeriveJsonEncoder.gen[Options]
//   given JsonDecoder[SdJwtPresentationPayload] = DeriveJsonDecoder.gen[SdJwtPresentationPayload]
//   given JsonEncoder[SdJwtPresentationPayload] = DeriveJsonEncoder.gen[SdJwtPresentationPayload]
// }
