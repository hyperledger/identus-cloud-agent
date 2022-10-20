package io.iohk.atala.pollux.service

import zio.{Task, ZIO, ZLayer}
import io.iohk.atala.pollux.schema.{VerifiableCredentialsSchemaInput, VerifiableCredentialsSchema}

import java.util.UUID

trait SchemaRegistryService {
  def createSchema(in: VerifiableCredentialsSchemaInput): Task[VerifiableCredentialsSchema]
  def getSchemaById(id: UUID): Task[Option[VerifiableCredentialsSchema]]
}

object SchemaRegistryService {
  def createSchema(
      in: VerifiableCredentialsSchemaInput
  ): ZIO[SchemaRegistryService, Throwable, VerifiableCredentialsSchema] =
    ZIO.serviceWithZIO[SchemaRegistryService](_.createSchema(in))

  def getSchemaById(id: UUID): ZIO[SchemaRegistryService, Throwable, Option[VerifiableCredentialsSchema]] =
    ZIO.serviceWithZIO[SchemaRegistryService](_.getSchemaById(id))
}
