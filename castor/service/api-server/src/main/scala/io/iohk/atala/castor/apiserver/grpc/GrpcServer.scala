package io.iohk.atala.castor.apiserver.grpc

import io.grpc.ServerBuilder
import zio.*

object GrpcServer {

  def start(port: Int): Task[Unit] = {
    val managedServer = ZIO.acquireRelease(
      for {
        _ <- ZIO.logInfo(s"staring grpc server on port $port")
        server <- ZIO.attempt {
          ServerBuilder
            .forPort(port)
            .build()
            .start()
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
