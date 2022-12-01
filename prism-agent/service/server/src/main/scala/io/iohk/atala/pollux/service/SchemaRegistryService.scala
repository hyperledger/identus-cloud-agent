package io.iohk.atala.pollux.service

import io.iohk.atala.api.http.model.{Order, Pagination}
import zio.{Task, ZIO, ZLayer}
import io.iohk.atala.pollux.schema.model.{
  VerifiableCredentialSchema,
  VerifiableCredentialSchemaPage,
  VerificationCredentialSchemaInput
}

import java.util.UUID

trait SchemaRegistryService {
  def createSchema(
      in: VerificationCredentialSchemaInput
  ): Task[VerifiableCredentialSchema]
  def getSchemaById(id: UUID): Task[Option[VerifiableCredentialSchema]]

  def lookupSchemas(
      filter: VerifiableCredentialSchema.Filter,
      pagination: Pagination,
      order: Option[Order]
  ): Task[VerifiableCredentialSchemaPage]
}

object SchemaRegistryService {
  def createSchema(
      in: VerificationCredentialSchemaInput
  ): ZIO[SchemaRegistryService, Throwable, VerifiableCredentialSchema] =
    ZIO.serviceWithZIO[SchemaRegistryService](_.createSchema(in))

  def getSchemaById(id: UUID): ZIO[SchemaRegistryService, Throwable, Option[
    VerifiableCredentialSchema
  ]] =
    ZIO.serviceWithZIO[SchemaRegistryService](_.getSchemaById(id))

  def lookupSchemas(
      filter: VerifiableCredentialSchema.Filter,
      pagination: Pagination,
      order: Option[Order]
  ): ZIO[
    SchemaRegistryService,
    Throwable,
    VerifiableCredentialSchemaPage
  ] =
    ZIO.serviceWithZIO[SchemaRegistryService](
      _.lookupSchemas(filter, pagination, order)
    )
}
