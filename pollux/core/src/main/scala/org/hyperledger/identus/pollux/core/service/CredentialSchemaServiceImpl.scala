package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError
import org.hyperledger.identus.pollux.core.model.schema.CredentialSchema
import org.hyperledger.identus.pollux.core.model.schema.CredentialSchema.FilteredEntries
import org.hyperledger.identus.pollux.core.repository.CredentialSchemaRepository
import org.hyperledger.identus.pollux.core.repository.Repository.SearchQuery
import org.hyperledger.identus.pollux.core.service.CredentialSchemaService.*
import zio.*
import zio.{IO, URLayer, ZLayer}

import java.util.UUID

class CredentialSchemaServiceImpl(
    credentialSchemaRepository: CredentialSchemaRepository
) extends CredentialSchemaService {
  override def create(in: CredentialSchema.Input): Result[CredentialSchema] = {
    for {
      credentialSchema <- CredentialSchema.make(in)
      _ <- CredentialSchema.validateCredentialSchema(credentialSchema)
      createdCredentialSchema <- credentialSchemaRepository.create(credentialSchema)
    } yield createdCredentialSchema
  }.mapError { (e: CredentialSchemaError) =>
    CredentialSchemaValidationError(e)
  }

  override def getByGUID(guid: UUID): IO[CredentialSchemaService.Error, CredentialSchema] = {
    for {
      resultOpt <- credentialSchemaRepository.findByGuid(guid)
      result <- ZIO.fromOption(resultOpt).mapError(_ => GuidNotFoundError(guid))
    } yield result
  }

  def getBy(
      author: String,
      id: UUID,
      version: String
  ): Result[CredentialSchema] = {
    getByGUID(CredentialSchema.makeGUID(author, id, version))
  }

  override def update(
      guid: UUID,
      in: CredentialSchema.Input
  ): Result[CredentialSchema] = {
    for {
      cs <- CredentialSchema.make(guid, in)
      _ <- CredentialSchema.validateCredentialSchema(cs).mapError(CredentialSchemaValidationError.apply)
      existingVersions <- credentialSchemaRepository.getAllVersions(guid, in.author)
      _ <- ZIO.fromOption(existingVersions.headOption).mapError(_ => GuidNotFoundError(guid))
      _ <- existingVersions.find(_ > in.version) match {
        case Some(higherVersion) =>
          ZIO.fail(
            UpdateError(
              guid,
              in.version,
              in.author,
              s"Higher version is found: $higherVersion"
            )
          )
        case None =>
          ZIO.succeed(cs)
      }
      _ <- existingVersions.find(_ == in.version) match {
        case Some(existingVersion) =>
          ZIO.fail(
            UpdateError(
              guid,
              in.version,
              in.author,
              s"The version already exists: $existingVersion"
            )
          )
        case None => ZIO.succeed(cs)
      }
      updated <- credentialSchemaRepository.create(cs)
    } yield updated
  }
  override def delete(guid: UUID): Result[CredentialSchema] = {
    credentialSchemaRepository.delete(guid)
  }

  override def lookup(
      filter: CredentialSchema.Filter,
      skip: Int,
      limit: Int
  ): Result[CredentialSchema.FilteredEntries] = {
    credentialSchemaRepository
      .search(SearchQuery(filter, skip, limit))
      .map(sr => FilteredEntries(sr.entries, sr.count.toInt, sr.totalCount.toInt))
  }
}

object CredentialSchemaServiceImpl {
  val layer: URLayer[CredentialSchemaRepository, CredentialSchemaService] =
    ZLayer.fromFunction(CredentialSchemaServiceImpl(_))
}
