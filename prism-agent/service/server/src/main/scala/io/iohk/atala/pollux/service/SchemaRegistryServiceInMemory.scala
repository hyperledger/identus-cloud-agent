package io.iohk.atala.pollux.service

import io.iohk.atala.api.http.model.{CollectionStats, Order, Pagination, PaginationInput}
import io.iohk.atala.pollux.schema.model.{
  VerifiableCredentialSchema,
  VerifiableCredentialSchemaInput,
  VerifiableCredentialSchemaPage
}
import zio.{Ref, Task, UIO, ZIO, ZLayer}

import java.util.UUID
import scala.collection.mutable

class SchemaRegistryServiceInMemory(
    ref: Ref[Map[UUID, VerifiableCredentialSchema]]
) extends SchemaRegistryService {

  // TODO: Figure out what is the logic for trying to overwrite the schema with the same id (409 Conflict)
  // TODO: Other validations (same [schema_name, version], list of the attributes is not empty, etc)
  override def createSchema(
      in: VerifiableCredentialSchemaInput
  ): Task[VerifiableCredentialSchema] = {
    val schema = VerifiableCredentialSchema(in)
    for {
      _ <- ref.update(s => s + (schema.id -> schema))
    } yield schema
  }

  override def getSchemaById(
      id: UUID
  ): Task[Option[VerifiableCredentialSchema]] = {
    for {
      storage <- ref.get
      schema = storage.get(id)
    } yield schema
  }

  // TODO: this is naive implementation for demo purposes, sorting doesn't work
  override def lookupSchemas(
      filter: VerifiableCredentialSchema.Filter,
      pagination: Pagination,
      order: Option[Order]
  ): Task[(VerifiableCredentialSchemaPage, CollectionStats)] = {
    for {
      storage: Map[UUID, VerifiableCredentialSchema] <- ref.get
      totalCount = storage.count(_ => true)
      filtered = storage.values.filter(filter.predicate).toList
      filteredCount = filtered.length
      paginated = filtered
        .slice(
          pagination.offset,
          pagination.offset + pagination.limit
        )
    } yield (
      VerifiableCredentialSchemaPage(contents = paginated),
      CollectionStats(totalCount, filteredCount)
    )
  }
}

object SchemaRegistryServiceInMemory {
  val layer = ZLayer.fromZIO(
    Ref
      .make(Map.empty[UUID, VerifiableCredentialSchema])
      .map(SchemaRegistryServiceInMemory(_))
  )
}
