package io.iohk.atala.mercury.mediator

import zio._
import io.iohk.atala.mercury.model.Message
import io.iohk.atala.mercury.model.DidId

enum MediationState:
  case Requested extends MediationState
  case Granted extends MediationState
  case Denied extends MediationState

trait ConnectionStorage {
  def store(id: DidId, msg: MediationState): UIO[MediationState]
  def get(id: DidId): UIO[Option[MediationState]]
}

final case class InMemoryConnectionStorage(private var bd: Map[DidId, MediationState]) extends ConnectionStorage {
  def store(id: DidId, msg: MediationState): UIO[MediationState] =
    ZIO.succeed {
      bd = bd + (id -> msg)
      msg
    }
      <* ZIO.logInfo(s"InMemoryConnectionStorage: $bd")

  def get(id: DidId): UIO[Option[MediationState]] =
    ZIO.succeed { bd.get(id) }
}

object ConnectionStorage {

  val layer: ULayer[ConnectionStorage] = {
    ZLayer.succeedEnvironment(
      ZEnvironment(InMemoryConnectionStorage(Map.empty))
    )
  }

  def store(id: DidId, msg: MediationState): URIO[ConnectionStorage, MediationState] =
    ZIO.serviceWithZIO(_.store(id, msg))

  def get(id: DidId): URIO[ConnectionStorage, Option[MediationState]] =
    ZIO.serviceWithZIO(_.get(id))

}
