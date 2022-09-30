package io.iohk.atala.iris.server.grpc

import io.grpc.ServerServiceDefinition
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import zio.*

object GrpcServices {

  def services: URIO[IrisServiceGrpc.IrisService, Seq[ServerServiceDefinition]] =
    for {
      ec <- ZIO.executor.map(_.asExecutionContext)
      irisService <- ZIO.serviceWith[IrisServiceGrpc.IrisService](IrisServiceGrpc.bindService(_, ec))
    } yield Seq(irisService)

}
