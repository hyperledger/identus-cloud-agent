package io.iohk.atala.mercury.mediator

import zio._
import io.iohk.atala.mercury.model.Message
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

// final case class MyDB() {
//   def store(id: DidId, msg: MessageString): UIO[Unit] = ZIO.succeed {
//     val newValue = bd + (id -> (bd.getOrElse(id, Seq.empty) :+ msg))
//     println("--" * 150)
//     println(newValue.mapValues(_.size).toMap)
//     bd = newValue
//   }

//   def get(id: DidId): UIO[Seq[MessageString]] = ZIO.succeed {
//     println("%" * 150)
//     println(bd.mapValues(_.size).toMap)
//     println(id)
//     println(bd.get(id))
//     bd.get(id).toSeq.flatten
//   }
// }

object MyDB {

  var bd: Map[DidId, Seq[MessageString]] = Map.empty // THIS IS NOT THREAD SAFE

  // val live2 = ZIO.stateful(MyDB(Map.empty))
  // val live = ZLayer.succeed(MyDB())

  // def store(id: DidId, msg: MessageString): URIO[MyDB, Unit] =
  //   ZIO.serviceWithZIO(_.store(id, msg))

  // def get(id: DidId): URIO[MyDB, Seq[MessageString]] =
  //   ZIO.serviceWithZIO(_.get(id))

  def store(id: DidId, msg: MessageString): UIO[Unit] = ZIO.succeed {
    val newValue = bd + (id -> (bd.getOrElse(id, Seq.empty) :+ msg))
    println("--" * 150)
    println(newValue.mapValues(_.size).toMap)
    bd = newValue
  }

  def get(id: DidId): UIO[Seq[MessageString]] = ZIO.succeed {
    println("%" * 150)
    println(bd.mapValues(_.size).toMap)
    println(id)
    println(bd.get(id))
    bd.get(id).toSeq.flatten
  }

}
