package io.iohk.atala.castor.apiserver.grpc.service

import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.proto.castor_api.{DIDServiceGrpc, Ping, Pong}
import zio.*

import scala.concurrent.Future

class DIDServiceGrpcImpl(service: DIDService)(using runtime: Runtime[Any]) extends DIDServiceGrpc.DIDService {

  override def sendPing(request: Ping): Future[Pong] = Unsafe.unsafe { implicit unsafe =>
    runtime.unsafe.runToFuture(ZIO.succeed(Pong("hello world")))
  }

}

object DIDServiceGrpcImpl {
  val layer: URLayer[DIDService, DIDServiceGrpc.DIDService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[DIDService]
    } yield DIDServiceGrpcImpl(svc)(using rt)
  }
}
