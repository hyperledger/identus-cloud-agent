package io.iohk.atala.iris.apiserver.grpc

import io.grpc.{ServerBuilder, ServerServiceDefinition}
import io.grpc.protobuf.services.ProtoReflectionService
import io.iohk.atala.iris.proto.iris_api.IrisServiceGrpc
import zio.*

object GrpcServer {

  def start(port: Int, services: Seq[ServerServiceDefinition]): Task[Unit] = {
    val managedServer = ZIO.acquireRelease(
      for {
        _ <- ZIO.logInfo(s"starting grpc server on port $port")
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
