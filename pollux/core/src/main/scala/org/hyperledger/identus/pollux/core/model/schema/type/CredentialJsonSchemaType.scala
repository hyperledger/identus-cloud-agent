package org.hyperledger.identus.pollux.core.model.schema.`type`

import org.hyperledger.identus.pollux.core.model.schema.validator.{JsonSchemaError, JsonSchemaValidatorImpl}
import org.hyperledger.identus.pollux.core.model.schema.Schema
import zio._
import zio.json._

object CredentialJsonSchemaType extends CredentialSchemaType {
  val VC_JSON_SCHEMA_URI = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json"

  override val `type`: String = VC_JSON_SCHEMA_URI

  override def validate(schema: Schema): IO[JsonSchemaError, Unit] =
    for {
      credentialJsonSchema <- CredentialJsonSchemaSerDesV1.schemaSerDes.deserialize(schema.toJson)
      _ <- JsonSchemaValidatorImpl.from(schema)
    } yield ()
}
