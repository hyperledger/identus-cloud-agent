package io.iohk.atala.pollux.service

import zio.{Task, ZIO, ZLayer, Ref, UIO}
import io.iohk.atala.pollux.schema.{VerifiableCredentialsSchemaInput, VerifiableCredentialsSchema}
import java.util.UUID
import scala.collection.mutable

class SchemaRegistryServiceInMemory(ref: Ref[Map[UUID, VerifiableCredentialsSchema]]) extends SchemaRegistryService {

  // TODO: Figure out what is the logic for trying to overwrite the schema with the same id (409 Conflict)
  // TODO: Other validations (same [schema_name, version], list of the attributes is not empty, etc)
  override def createSchema(in: VerifiableCredentialsSchemaInput): Task[VerifiableCredentialsSchema] = {
    val schema = VerifiableCredentialsSchema(in)
    for {
      _ <- ref.update(s => s + (schema.id -> schema))
    } yield schema
  }

  override def getSchemaById(id: UUID): Task[Option[VerifiableCredentialsSchema]] = {
    for {
      storage <- ref.get
      schema = storage.get(id)
    } yield schema
  }
}

object SchemaRegistryServiceInMemory {
  val layer = ZLayer.fromZIO(
    Ref.make(Map.empty[UUID, VerifiableCredentialsSchema]).map(SchemaRegistryServiceInMemory(_))
  )
}
