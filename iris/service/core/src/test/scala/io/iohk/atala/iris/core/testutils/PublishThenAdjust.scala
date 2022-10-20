package io.iohk.atala.iris.core.testutils

import io.iohk.atala.iris.core.service.InmemoryUnderlyingLedgerService
import zio.*
import zio.test.*
import io.iohk.atala.iris.proto.dlt as proto

case class PublishThenAdjust(operations: Seq[proto.IrisOperation], adjust: Duration)

object PublishThenAdjust {
  implicit class Then(operations: Seq[proto.IrisOperation]) {
    def >>(adj: Duration): PublishThenAdjust = PublishThenAdjust(operations, adj)
  }

  def foreachZIO[R](srv: InmemoryUnderlyingLedgerService)(xs: Iterable[PublishThenAdjust]): ZIO[R, Any, Unit] =
    ZIO.foreachDiscard[R, Any, PublishThenAdjust](xs) { case PublishThenAdjust(ops, adj) =>
      srv.publish(ops).flatMap(_ => TestClock.adjust(adj))
    }
}
