package io.iohk.atala.iris.core.mock

import io.iohk.atala.iris.core.repository.DbRepositoryTransactor
import zio.*

object DummyDbRepositoryTransactor {
  val layer: ULayer[DbRepositoryTransactor[Task]] = ZLayer.succeed(DummyDbRepositoryTransactor())
}

class DummyDbRepositoryTransactor extends DbRepositoryTransactor[Task] {
  override def runAtomically[A](action: Task[A]): Task[A] = action
}
