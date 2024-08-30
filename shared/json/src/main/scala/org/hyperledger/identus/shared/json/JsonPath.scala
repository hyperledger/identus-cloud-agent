package org.hyperledger.identus.shared.json

import com.jayway.jsonpath.{InvalidPathException, JsonPath as JaywayJsonPath, PathNotFoundException}
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import org.hyperledger.identus.shared.models.{Failure, StatusCode}
import zio.*
import zio.json.*
import zio.json.ast.Json

sealed trait JsonPathError extends Failure {
  override def namespace: String = "JsonPathError"
}

object JsonPathError {
  final case class InvalidPathInput(msg: String) extends JsonPathError {
    override def statusCode: StatusCode = StatusCode.BadRequest
    override def userFacingMessage: String = s"The JsonPath input is not valid: $msg"
  }

  final case class PathNotFound(path: String) extends JsonPathError {
    override def statusCode: StatusCode = StatusCode.BadRequest
    override def userFacingMessage: String = s"The json path '$path' cannot be found in a json"
  }
}

opaque type JsonPath = JaywayJsonPath

object JsonPath {
  def compile(path: String): IO[JsonPathError, JsonPath] = {
    ZIO
      .attempt(JaywayJsonPath.compile(path))
      .refineOrDie {
        case e: IllegalArgumentException => JsonPathError.InvalidPathInput(e.getMessage())
        case e: InvalidPathException     => JsonPathError.InvalidPathInput(e.getMessage())
      }
  }

  extension (jsonPath: JsonPath) {
    def read(json: Json): IO[JsonPathError, Json] = {
      val jsonProvider = JacksonJsonProvider()
      val document = JaywayJsonPath.parse(json.toString())
      for {
        queriedObj <- ZIO
          .attempt(document.read[java.lang.Object](jsonPath))
          .refineOrDie { case e: PathNotFoundException =>
            JsonPathError.PathNotFound(jsonPath.getPath())
          }
        queriedJsonStr = jsonProvider.toJson(queriedObj)
        queriedJson <- ZIO.fromEither(queriedJsonStr.fromJson[Json]).orDieWith(Exception(_))
      } yield queriedJson
    }
  }
}
