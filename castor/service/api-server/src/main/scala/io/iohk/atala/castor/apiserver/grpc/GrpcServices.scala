package io.iohk.atala.castor.apiserver.grpc

import akka.actor.typed.ActorSystem
import io.grpc.ServerServiceDefinition
import io.iohk.atala.castor.proto.castor_api.DIDServiceGrpc
import zio.*

object GrpcServices {

  def services: URIO[DIDServiceGrpc.DIDService, Seq[ServerServiceDefinition]] =
    for {
      ec <- ZIO.executor.map(_.asExecutionContext)
      didService <- ZIO.serviceWith[DIDServiceGrpc.DIDService](DIDServiceGrpc.bindService(_, ec))
    } yield Seq(didService)

}
