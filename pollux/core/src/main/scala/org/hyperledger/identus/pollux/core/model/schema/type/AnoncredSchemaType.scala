package org.hyperledger.identus.pollux.core.model.schema.`type`

import com.networknt.schema._
import org.hyperledger.identus.pollux.core.model.schema.`type`.anoncred.AnoncredSchemaSerDesV1
import org.hyperledger.identus.pollux.core.model.schema.`type`.anoncred.AnoncredSchemaSerDesV1._
import org.hyperledger.identus.pollux.core.model.schema.validator.{
  JsonSchemaError,
  JsonSchemaUtils,
  JsonSchemaValidatorImpl,
  SchemaSerDes
}
import org.hyperledger.identus.pollux.core.model.schema.Schema
import zio._
import zio.json._

object AnoncredSchemaType extends CredentialSchemaType {

  val anondcredShemaBasedSerDes: SchemaSerDes[AnoncredSchemaSerDesV1] = AnoncredSchemaSerDesV1.schemaSerDes
  val `type`: String = AnoncredSchemaSerDesV1.version

  override def validate(schema: Schema): IO[JsonSchemaError, Unit] = {
    for {
      jsonSchemaSchema <- anondcredShemaBasedSerDes.initialiseJsonSchema
      schemaValidator = JsonSchemaValidatorImpl(jsonSchemaSchema)
      jsonSchemaNode <- JsonSchemaUtils.toJsonNode(schema)
      _ <- schemaValidator.validate(jsonSchemaNode)
    } yield ()
  }
}
