package io.iohk.atala.pollux.services

import io.iohk.atala.pollux.models.*
import zio.Task
import java.util.UUID

trait SchemaService {
  def createSchema(in: VerifiableCredentialsSchemaInput): Task[VerifiableCredentialsSchema]
  def getSchemaById(id: UUID): Task[Option[VerifiableCredentialsSchema]]
}
