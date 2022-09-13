package io.iohk.atala.iris.apiserver.grpc.service

import io.iohk.atala.iris.core.service.PublishingService
import io.iohk.atala.iris.core.worker.PublishingScheduler
import io.iohk.atala.iris.proto.iris_api.{IrisServiceGrpc, Ping, Pong}
import zio.*

import scala.concurrent.Future

class IrisServiceGrpcImpl(service: PublishingScheduler)(using runtime: Runtime[Any]) extends IrisServiceGrpc.IrisService {

  override def sendPing(request: Ping): Future[Pong] = Unsafe.unsafe { implicit unsafe =>
    runtime.unsafe.runToFuture(ZIO.succeed(Pong("hello world")))
  }

}

object IrisServiceGrpcImpl {
  val layer: URLayer[PublishingScheduler, IrisServiceGrpc.IrisService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[PublishingScheduler]
    } yield IrisServiceGrpcImpl(svc)(using rt)
  }
}
