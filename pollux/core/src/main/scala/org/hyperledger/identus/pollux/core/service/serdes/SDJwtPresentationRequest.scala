package org.hyperledger.identus.pollux.core.service.serdes

import zio.json.*
import org.hyperledger.identus.pollux.core.model.presentation.Options

case class SDJwtPresentation(options: Options, claims: ast.Json.Obj)

object SDJwtPresentation {
  given JsonDecoder[Options] = DeriveJsonDecoder.gen[Options]
  given JsonEncoder[Options] = DeriveJsonEncoder.gen[Options]

  given JsonDecoder[SDJwtPresentation] = DeriveJsonDecoder.gen[SDJwtPresentation]
  given JsonEncoder[SDJwtPresentation] = DeriveJsonEncoder.gen[SDJwtPresentation]
}
