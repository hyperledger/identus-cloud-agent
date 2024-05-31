package org.hyperledger.identus.pollux.vc.jwt.schema

import io.circe
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.Encoder
import io.circe.Json
import net.reactivecore.cjs.DocumentValidator
import net.reactivecore.cjs.Loader
import zio.prelude.*
import zio.NonEmptyChunk

class ReactiveCoreSchemaValidator(documentValidator: DocumentValidator) extends SchemaValidator {
  override def validate(payloadToValidate: Json): Validation[String, Json] =
    NonEmptyChunk
      .fromIterableOption(
        documentValidator.validate(payloadToValidate.asJson).violations.map(_.toString)
      )
      .fold(Validation.succeed(payloadToValidate))(Validation.failNonEmptyChunk)
}
object ReactiveCoreSchemaValidator {
  def fromSchema(schema: Json): Either[String, ReactiveCoreSchemaValidator] =
    Loader.empty.fromJson(schema).left.map(_.message).map(a => ReactiveCoreSchemaValidator(a))
}
