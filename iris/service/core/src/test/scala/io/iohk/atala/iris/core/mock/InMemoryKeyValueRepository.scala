package io.iohk.atala.iris.core.mock

import io.iohk.atala.iris.core.repository.KeyValueRepository
import zio.*

object InMemoryKeyValueRepository {
  val layer: ULayer[KeyValueRepository[Task]] = ZLayer.fromZIO {
    for {
      ref <- Ref.make(Map[String, Any]())
      srv = InMemoryKeyValueRepository(ref)
    } yield srv
  }
}

class InMemoryKeyValueRepository(kv: Ref[Map[String, Any]]) extends KeyValueRepository[Task] {
  override def get(key: String): Task[Option[String]] = kv.get.map(_.get(key).map(_.asInstanceOf[String]))

  override def getInt(key: String): Task[Option[Int]] = kv.get.map(_.get(key).map(_.asInstanceOf[Int]))

  override def set(key: String, value: Option[Any]): Task[Unit] = kv.update(_.updatedWith(key)(_ => value))
}
