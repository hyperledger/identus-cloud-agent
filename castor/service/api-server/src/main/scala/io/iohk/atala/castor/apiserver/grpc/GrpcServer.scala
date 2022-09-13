package io.iohk.atala.castor.apiserver.grpc

import io.grpc.{ServerBuilder, ServerServiceDefinition}
import io.grpc.protobuf.services.ProtoReflectionService
import io.iohk.atala.castor.proto.castor_api.DIDServiceGrpc.DIDService
import zio.*

object GrpcServer {

  def start(port: Int, services: Seq[ServerServiceDefinition]): Task[Unit] = {
    val managedServer = ZIO.acquireRelease(
      for {
        _ <- ZIO.logInfo(s"staring grpc server on port $port")
        server <- ZIO.attempt {
          val builder = ServerBuilder.forPort(port)
          services.foreach(s => builder.addService(s))
          builder.addService(ProtoReflectionService.newInstance())
          builder.build().start()
        }
        _ <- ZIO.logInfo(s"grpc server listening on port $port")
      } yield server
    )(server =>
      for {
        _ <- ZIO.logInfo("stopping grpc server")
        _ <- ZIO.attempt(server.shutdown()).orDie
        _ <- ZIO.logInfo("grpc server stopped successfully")
      } yield ()
    )

    ZIO.scoped {
      for {
        _ <- managedServer
        _ <- ZIO.never
      } yield ()
    }
  }

}
