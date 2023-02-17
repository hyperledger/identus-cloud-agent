package io.iohk.atala.agent.server

import io.iohk.atala.agent.server.Modules
import io.iohk.atala.mercury.*
import io.iohk.atala.pollux.schema.controller.VerificationPolicyControllerInMemory
import io.iohk.atala.resolvers.UniversalDidResolver
import org.didcommx.didcomm.DIDComm
import zio.*
import io.iohk.atala.pollux.service.SchemaRegistryServiceInMemory

object MainInMemory extends ZIOAppDefault {

  override def run: ZIO[Any, Throwable, Unit] =
    for {
      _ <- Modules.zioApp
        .provide(
          SchemaRegistryServiceInMemory.layer,
          VerificationPolicyControllerInMemory.layer,
          SystemModule.configLayer
        )
        .fork
      _ <- ZIO.never
    } yield ()

}
