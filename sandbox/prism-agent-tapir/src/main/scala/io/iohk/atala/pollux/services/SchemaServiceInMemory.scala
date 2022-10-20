package io.iohk.atala.pollux.services
import io.iohk.atala.pollux.models.{VerifiableCredentialsSchema, VerifiableCredentialsSchemaInput}

import java.util.UUID
import scala.collection.mutable
import zio.{Task, ZIO}

class SchemaServiceInMemory() extends SchemaService {
  val storage = mutable.Map[UUID, VerifiableCredentialsSchema]()

  override def createSchema(in: VerifiableCredentialsSchemaInput): Task[VerifiableCredentialsSchema] = {
    val schema = VerifiableCredentialsSchema(in)
    storage.put(schema.id, schema)
    ZIO.succeed(schema)
  }

  override def getSchemaById(id: UUID): Task[Option[VerifiableCredentialsSchema]] = {
    ZIO.succeed(storage.get(id))
  }
}

object SchemaServiceInMemory {
  val instance = SchemaServiceInMemory()
}
