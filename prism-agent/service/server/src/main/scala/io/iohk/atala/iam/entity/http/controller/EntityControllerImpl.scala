package io.iohk.atala.iam.entity.http.controller

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.agent.walletapi.service.EntityService
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.iam.entity.http.model.{CreateEntityRequest, EntityResponse, EntityResponsePage}
import zio.ZIO.succeed
import zio.{IO, URLayer, ZLayer}

import java.util.UUID

class EntityControllerImpl(service: EntityService) extends EntityController {
  override def createEntity(in: CreateEntityRequest)(implicit rc: RequestContext): IO[ErrorResponse, EntityResponse] = {
    val id = in.id.getOrElse(UUID.randomUUID())
    val walletId = in.walletId.getOrElse(Entity.ZeroWalletId)
    for {
      entityToCreate <- succeed(Entity(id, in.name, walletId))
      createdEntity <- service.create(entityToCreate)
      self = rc.request.uri.addPath(createdEntity.id.toString).toString
    } yield EntityResponse.fromDomain(createdEntity).withSelf(self)
  } mapError (EntityController.domainToHttpError)

  override def getEntity(id: UUID)(implicit rc: RequestContext): IO[ErrorResponse, EntityResponse] = {
    for {
      entity <- service.getById(id)
      self = rc.request.uri.toString
    } yield EntityResponse.fromDomain(entity).withSelf(self)
  } mapError (EntityController.domainToHttpError)

  // TODO: add the missing pagination fields to the response
  override def getEntities(paginationIn: PaginationInput)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, EntityResponsePage] = {
    for {
      entities <- service.getAll(paginationIn.offset, paginationIn.limit)
      self = rc.request.uri.toString
    } yield EntityResponsePage.fromDomain(entities).withSelf(self)
  } mapError (EntityController.domainToHttpError)

  override def updateEntityName(id: UUID, name: String)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, EntityResponse] = {
    for {
      _ <- service.updateName(id, name)
      updatedEntity <- service.getById(id)
      self = rc.request.uri.toString
    } yield EntityResponse.fromDomain(updatedEntity).withSelf(self)
  } mapError (EntityController.domainToHttpError)

  override def updateEntityWalletId(id: UUID, walletId: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, EntityResponse] = {
    for {
      _ <- service.assignWallet(id, walletId)
      updatedEntity <- service.getById(id)
      self = rc.request.uri.toString
    } yield EntityResponse.fromDomain(updatedEntity).withSelf(self)
  } mapError (EntityController.domainToHttpError)

  override def deleteEntity(id: UUID)(implicit rc: RequestContext): IO[ErrorResponse, Unit] = {
    for {
      _ <- service.deleteById(id)
    } yield ()
  } mapError (EntityController.domainToHttpError)
}

object EntityControllerImpl {
  val layer: URLayer[EntityService, EntityController] =
    ZLayer.fromFunction(new EntityControllerImpl(_))
}
