package io.iohk.atala.iris.sql.repository

import doobie.*
import io.iohk.atala.iris.core.repository.KeyValueRepository
import zio.*

class JdbcKeyValueRepositoryIO extends KeyValueRepository[ConnectionIO] {
  override def get(key: String): ConnectionIO[Option[String]] = ???

  override def getInt(key: String): ConnectionIO[Option[Int]] = ???

  override def set(key: String, value: Option[Int | String]): ConnectionIO[Unit] = ???
}

object JdbcKeyValueRepositoryIO {
  val layer: ULayer[KeyValueRepository[ConnectionIO]] =
    ZLayer.succeed(new JdbcKeyValueRepositoryIO)
}

class JdbcKeyValueRepository(xa: Transactor[Task], ioImpl: KeyValueRepository[ConnectionIO])
    extends KeyValueRepository[Task] {

  override def get(key: String): Task[Option[String]] = ???

  override def getInt(key: String): Task[Option[RuntimeFlags]] = ???

  override def set(key: String, value: Option[Int | String]): Task[Unit] = ???
}

object JdbcKeyValueRepository {
  val layer: URLayer[Transactor[Task] & KeyValueRepository[ConnectionIO], KeyValueRepository[Task]] =
    ZLayer.fromFunction(new JdbcKeyValueRepository(_, _))
}
