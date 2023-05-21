package io.iohk.atala.api.http.codec

import io.circe.Json as CirceJson
import sttp.tapir.Schema
import sttp.tapir.json.zio.*
import zio.json.ast.Json as ZioJson
import zio.json.internal.Write
import zio.json.*

object CirceJsonInterop {

  private def toZioJsonAst(circeJson: CirceJson): ZioJson = {
    val encoded = circeJson.noSpaces
    encoded.fromJson[ZioJson] match {
      case Left(failure) =>
        throw Exception(s"Circe and Zio Json interop fail. Unable to  convert from Circe to Zio AST. $failure")
      case Right(value) => value
    }
  }

  private def toCirceJsonAst(zioJson: ZioJson): CirceJson = {
    val encoded = zioJson.toJson
    io.circe.parser.parse(encoded).left.map(_.toString) match {
      case Left(failure) =>
        throw Exception(s"Circe and Zio Json interop fail. Unable to  convert from Zio to Circe AST. $failure")
      case Right(value) => value
    }
  }

  given encodeJson: JsonEncoder[CirceJson] = JsonEncoder[ZioJson].contramap(toZioJsonAst)

  given decodeJson: JsonDecoder[CirceJson] = JsonDecoder[ZioJson].map(toCirceJsonAst)

  given schemaJson: Schema[CirceJson] =
    Schema.derived[ZioJson].map[CirceJson](js => Some(toCirceJsonAst(js)))(toZioJsonAst)

}
