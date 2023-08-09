//package io.iohk.atala.agent.walletapi.service
//import io.iohk.atala.agent.walletapi.model.Entity
//import io.iohk.atala.agent.walletapi.model.error.EntityServiceError
//import io.iohk.atala.agent.walletapi.sql.EntityRepository
//import zio.*
//import zio.mock.*
//
//import java.util.UUID
//
//object EntityServiceSpec {
//
//}
//
//object MockEntityRepository extends Mock[EntityRepository] {
//  object Insert extends Effect[Entity, EntityServiceError, Entity]
//  object GetById extends Effect[UUID, EntityServiceError, Entity]
//
//  object UpdateName extends Effect[(UUID, String), EntityServiceError, Unit]
//
//  object UpdateWallet extends Effect[(UUID, UUID), EntityServiceError, Unit]
//
//  object Delete extends Effect[UUID, EntityServiceError, Unit]
//
//  val compose: URLayer[Proxy, EntityRepository] =
//    ZLayer {
//      for {
//        proxy <- ZIO.service[Proxy]
//      } yield new EntityRepository {
//        override def insert(entity: Entity): IO[EntityServiceError, Entity] =
//          proxy(Insert, entity)
//        override def getById(id: UUID): IO[EntityServiceError, Entity] =
//          proxy(GetById, id)
//
//        override def updateName(entityId: UUID, name: String): IO[EntityServiceError, Unit] = proxy(UpdateName, (entityId, name))
//
//        override def updateWallet(entityId: UUID, walletId: UUID): IO[EntityServiceError, Unit] = proxy(UpdateWallet, (entityId, walletId)
//
//        override def delete(id: UUID): IO[EntityServiceError, Unit] = proxy(Delete,id)
//      }
//    }
//}
