package org.hyperledger.identus.shared.json

import io.circe.Json as CirceJson
import zio.json.*
import zio.json.ast.Json as ZioJson

object JsonInterop {
  def toZioJsonAst(circeJson: CirceJson): ZioJson = {
    val encoded = circeJson.noSpaces
    encoded.fromJson[ZioJson] match {
      case Left(failure) =>
        throw Exception(s"Circe and Zio Json interop fail. Unable to convert from Circe to Zio AST. $failure")
      case Right(value) => value
    }
  }

  def toCirceJsonAst(zioJson: ZioJson): CirceJson = {
    val encoded = zioJson.toJson
    io.circe.parser.parse(encoded).left.map(_.toString) match {
      case Left(failure) =>
        throw Exception(s"Circe and Zio Json interop fail. Unable to convert from Zio to Circe AST. $failure")
      case Right(value) => value
    }
  }
}
