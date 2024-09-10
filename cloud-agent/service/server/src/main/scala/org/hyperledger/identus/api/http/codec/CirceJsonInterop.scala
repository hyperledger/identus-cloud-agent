package org.hyperledger.identus.api.http.codec

import io.circe.Json as CirceJson
import org.hyperledger.identus.shared.json.JsonInterop
import sttp.tapir.json.zio.*
import sttp.tapir.Schema
import zio.json.*
import zio.json.ast.Json as ZioJson

object CirceJsonInterop {
  given encodeJson: JsonEncoder[CirceJson] = JsonEncoder[ZioJson].contramap(JsonInterop.toZioJsonAst)
  given decodeJson: JsonDecoder[CirceJson] = JsonDecoder[ZioJson].map(JsonInterop.toCirceJsonAst)
  given schemaJson: Schema[CirceJson] =
    Schema.derived[ZioJson].map[CirceJson](js => Some(JsonInterop.toCirceJsonAst(js)))(JsonInterop.toZioJsonAst)
}
