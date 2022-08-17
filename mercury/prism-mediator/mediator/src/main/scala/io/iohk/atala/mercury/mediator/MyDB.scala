package io.iohk.atala.mercury.mediator

import zio._
import io.iohk.atala.mercury.model.Message
import io.iohk.atala.mercury.model.DidId

final case class MyDB(bd: Map[DidId, Seq[MessageString]])

type MessageString = String //FIXME Replace with Message
type ZDB = ZState[MyDB]

object MyDB {

  def live = ZState.initial(MyDB(Map.empty))

  def store(id: DidId, msg: MessageString): URIO[ZDB, Unit] =
    // TODO like ZIO.updateState[MyDB](state => state)
    ZIO.serviceWithZIO(_.update(unbox => {
      val newValue = unbox.bd + (id -> unbox.bd.getOrElse(id, Seq.empty))
      MyDB(newValue)
    }))

  def get(id: DidId): URIO[ZDB, Seq[MessageString]] =
    ZIO.serviceWithZIO(_.get.map(unbox => unbox.bd.get(id).toSeq.flatten))

}
