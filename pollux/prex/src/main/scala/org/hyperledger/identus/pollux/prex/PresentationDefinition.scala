package org.hyperledger.identus.pollux.prex

import com.networknt.schema.{JsonSchema, SpecVersion}
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.Json as CirceJson
import org.hyperledger.identus.shared.json.{JsonInterop, JsonPath, JsonPathError, JsonSchemaError, JsonSchemaUtils}
import zio.*
import zio.json.ast.Json as ZioJson

opaque type JsonPathValue = String

object JsonPathValue {
  given Encoder[JsonPathValue] = Encoder.encodeString
  given Decoder[JsonPathValue] = Decoder.decodeString
  given Conversion[String, JsonPathValue] = identity

  extension (jpv: JsonPathValue) {
    def toJsonPath: IO[JsonPathError, JsonPath] = JsonPath.compile(jpv)
    def value: String = jpv
  }
}

opaque type FieldFilter = ZioJson

object FieldFilter {
  given Encoder[FieldFilter] = Encoder.encodeJson.contramap(JsonInterop.toCirceJsonAst)
  given Decoder[FieldFilter] = Decoder.decodeJson.map(JsonInterop.toZioJsonAst)

  extension (f: FieldFilter)
    def asJsonZio: ZioJson = f
    def asJsonCirce: CirceJson = JsonInterop.toCirceJsonAst(f)

    // Json schema draft 7 must be used
    // https://identity.foundation/presentation-exchange/spec/v2.1.1/#json-schema
    def toJsonSchema: IO[JsonSchemaError, JsonSchema] =
      JsonSchemaUtils.jsonSchemaAtVersion(f.toString(), SpecVersion.VersionFlag.V7)
}

case class Field(
    id: Option[String] = None,
    path: Seq[JsonPathValue] = Seq.empty,
    name: Option[String] = None,
    purpose: Option[String] = None,
    filter: Option[FieldFilter] = None,
    optional: Option[Boolean] = None
)

object Field {
  given Encoder[Field] = deriveEncoder[Field]
  given Decoder[Field] = deriveDecoder[Field]
}

case class Jwt(alg: Seq[String])

object Jwt {
  given Encoder[Jwt] = deriveEncoder[Jwt]
  given Decoder[Jwt] = deriveDecoder[Jwt]
}

case class Ldp(proof_type: Seq[String])

object Ldp {
  given Encoder[Ldp] = deriveEncoder[Ldp]
  given Decoder[Ldp] = deriveDecoder[Ldp]
}

case class ClaimFormat(jwt: Option[Jwt] = None, ldp: Option[Ldp] = None)

object ClaimFormat {
  given Encoder[ClaimFormat] = deriveEncoder[ClaimFormat]
  given Decoder[ClaimFormat] = deriveDecoder[ClaimFormat]
}

case class Constraints(fields: Option[Seq[Field]])

object Constraints {
  given Encoder[Constraints] = deriveEncoder[Constraints]
  given Decoder[Constraints] = deriveDecoder[Constraints]
}

/** Refer to <a href="https://identity.foundation/presentation-exchange/#input-descriptor">Input Descriptors</a>
  */
case class InputDescriptor(
    id: String = java.util.UUID.randomUUID.toString(),
    name: Option[String] = None,
    purpose: Option[String] = None,
    format: Option[ClaimFormat] = None,
    constraints: Constraints
)

object InputDescriptor {
  given Encoder[InputDescriptor] = deriveEncoder[InputDescriptor]
  given Decoder[InputDescriptor] = deriveDecoder[InputDescriptor]
}

/** Refer to <a href="https://identity.foundation/presentation-exchange/#presentation-definition">Presentation
  * Definition</a>
  */
case class PresentationDefinition(
    id: String = java.util.UUID.randomUUID.toString(), // UUID
    input_descriptors: Seq[InputDescriptor] = Seq.empty,
    name: Option[String] = None,
    purpose: Option[String] = None,
    format: Option[ClaimFormat] = None
)

object PresentationDefinition {
  given Encoder[PresentationDefinition] = deriveEncoder[PresentationDefinition]
  given Decoder[PresentationDefinition] = deriveDecoder[PresentationDefinition]
}
