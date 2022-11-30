package io.iohk.atala.agent.server

import io.iohk.atala.agent.server.Modules
import io.iohk.atala.mercury.*
import io.iohk.atala.resolvers.UniversalDidResolver
import org.didcommx.didcomm.DIDComm
import zio.*

object MainInMemory extends ZIOAppDefault {

  override def run: ZIO[Any, Throwable, Unit] =
    for {
      _ <- Modules.zioApp.fork
      _ <- ZIO.never
    } yield ()

}
