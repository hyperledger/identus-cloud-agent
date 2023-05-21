package io.iohk.atala.api.http.codec

import io.circe.Json as CirceJson
import zio.json.ast.Json as ZioJson
import zio.json.internal.Write
import zio.json.*

object CirceJsonInterop {

  given encodeJson: JsonEncoder[CirceJson] = JsonEncoder[ZioJson].contramap { circeJson =>
    val encoded = circeJson.noSpaces
    encoded.fromJson[ZioJson] match {
      case Left(failure) =>
        throw Exception(s"Circe and Zio Json interop fail. Unable to encode from Circe Json. $failure")
      case Right(value) => value
    }
  }

  given decodeJson: JsonDecoder[CirceJson] = JsonDecoder[ZioJson].mapOrFail { zioJson =>
    val encoded = zioJson.toJson
    io.circe.parser.parse(encoded).left.map(_.toString)
  }

}
