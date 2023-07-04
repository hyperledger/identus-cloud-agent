package io.iohk.atala.mercury.mediator

import zio._
import io.iohk.atala.mercury.model.DidId

type MessageString = String //FIXME Replace with Message
// type ZDB = MyDB //ZState[MyDB]

// final case class MyDB(bd: Map[DidId, Seq[MessageString]])

// object MyDB {

//   // val live2 = ZIO.stateful(MyDB(Map.empty))
//   val live = ZState.initial(MyDB(Map.empty))

//   def store(id: DidId, msg: MessageString): URIO[ZDB, Unit] =
//     // TODO like ZIO.updateState[MyDB](state => state)
//     ZIO.serviceWithZIO(_.update(unbox => {
//       val newValue = unbox.bd + (id -> (unbox.bd.getOrElse(id, Seq.empty) :+ msg))
//       println("--" * 150)
//       println(newValue.mapValues(_.size))
//       MyDB(newValue)
//     }))

//   def get(id: DidId): URIO[ZDB, Seq[MessageString]] =
//     ZIO.serviceWithZIO(_.get.map { unbox =>
//       println(unbox)
//       println("%" * 150)
//       println(id)
//       println(unbox.bd.get(id))
//       unbox.bd.get(id).toSeq.flatten
//     })

// }

//TODO Rename to MailStorage
trait MailStorage {
  def store(id: DidId, msg: MessageString): UIO[Unit]
  def get(id: DidId): UIO[Seq[MessageString]]
}

final case class InmemoryMailStorage(private var bd: Map[DidId, Seq[MessageString]]) extends MailStorage {
  def store(id: DidId, msg: MessageString): UIO[Unit] =
    ZIO.succeed { bd = bd + (id -> (bd.getOrElse(id, Seq.empty) :+ msg)) }
      <* ZIO.logInfo("InmemoryMailStorage: " + bd.view.mapValues(_.size).toMap.toString)

  def get(id: DidId): UIO[Seq[MessageString]] =
    ZIO.succeed { bd.get(id).toSeq.flatten }
}

object MailStorage {

  val layer: ULayer[MailStorage] = {
    ZLayer.succeedEnvironment(
      ZEnvironment(InmemoryMailStorage(Map.empty))
    )
  }

  def store(id: DidId, msg: MessageString): URIO[MailStorage, Unit] =
    ZIO.serviceWithZIO(_.store(id, msg))

  def get(id: DidId): URIO[MailStorage, Seq[MessageString]] =
    ZIO.serviceWithZIO(_.get(id))

}
