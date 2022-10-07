package io.iohk.atala.mercury.mediator

import zio._
import io.iohk.atala.mercury.model.Message
import io.iohk.atala.mercury.model.DidId

enum MediationState:
  case Requested extends MediationState
  case Granted extends MediationState
  case Denied extends MediationState

trait ConnectionStorage {
  def store(id: DidId, msg: MediationState): UIO[Unit]
  def get(id: DidId): UIO[MediationState]
}

final case class InMemoryConnectionStorage(private var bd: Map[DidId, MediationState]) extends ConnectionStorage {
  def store(id: DidId, msg: MediationState): UIO[Unit] =
    ZIO.succeed { bd = bd + (id -> msg) }
      <* ZIO.logInfo(s"InMemoryConnectionStorage: $bd")

  def get(id: DidId): UIO[MediationState] =
    ZIO.succeed { bd.get(id).toSeq.flatten }
}

object ConnectionStorage {

  val layer: ULayer[ConnectionStorage] = {
    ZLayer.succeedEnvironment(
      ZEnvironment(InMemoryConnectionStorage(Map.empty))
    )
  }

  def store(id: DidId, msg: MediationState): URIO[ConnectionStorage, Unit] =
    ZIO.serviceWithZIO(_.store(id, msg))

  def get(id: DidId): URIO[ConnectionStorage, MediationState] =
    ZIO.serviceWithZIO(_.get(id))

}
