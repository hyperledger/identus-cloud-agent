package org.hyperledger.identus.pollux.core.model.schema.validator

import com.networknt.schema.JsonSchema
import org.hyperledger.identus.pollux.core.model.schema.validator.JsonSchemaError.*
import zio.json.*
import zio.json.ast.Json
import zio.json.ast.Json.*
import zio.{IO, ZIO}

class SchemaSerDes[S](jsonSchemaSchemaStr: String) {

  def initialiseJsonSchema: IO[JsonSchemaError, JsonSchema] =
    JsonSchemaUtils.jsonSchema(jsonSchemaSchemaStr)

  def serializeToJsonString(instance: S)(using encoder: JsonEncoder[S]): String = {
    instance.toJson
  }

  def serialize(instance: S)(using encoder: JsonEncoder[S]): Either[String, Json] = {
    instance.toJsonAST
  }

  def deserialize(
      schema: zio.json.ast.Json
  )(using decoder: JsonDecoder[S]): IO[JsonSchemaError, S] = {
    deserialize(schema.toString())
  }

  def deserialize(
      jsonString: String
  )(using decoder: JsonDecoder[S]): IO[JsonSchemaError, S] = {
    for {
      _ <- validate(jsonString)
      anoncredSchema <-
        ZIO
          .fromEither(decoder.decodeJson(jsonString))
          .mapError(JsonSchemaError.JsonSchemaParsingError.apply)
    } yield anoncredSchema
  }

  def deserializeAsJson(jsonString: String): IO[JsonSchemaError, Json] = {
    for {
      _ <- validate(jsonString)
      json <-
        ZIO
          .fromEither(jsonString.fromJson[Json])
          .mapError(JsonSchemaError.JsonSchemaParsingError.apply)
    } yield json
  }

  def validate(jsonString: String): IO[JsonSchemaError, Unit] = {
    for {
      jsonSchemaSchema <- JsonSchemaUtils.jsonSchema(jsonSchemaSchemaStr)
      schemaValidator = JsonSchemaValidatorImpl(jsonSchemaSchema)
      result <- schemaValidator.validate(jsonString)
    } yield result
  }

}
