package io.iohk.atala.mercury.resolvers

import zio._
import io.iohk.atala.mercury._
import org.didcommx.didcomm.DIDComm

object MediatorDidComm {
  val mediator: ZLayer[Any, Nothing, DidComm] = ZLayer.succeed(
    AgentService[Agent.Mediator.type](
      new DIDComm(
        io.iohk.atala.resolvers.UniversalDidResolver,
        MediatorSecretResolver.secretResolver
      ),
      Agent.Mediator
    )
  )

  val peerDidMediator: ZLayer[Any, Nothing, DidComm] = ZLayer.succeed(
    AgentService[Agent.Mediator.type](
      new DIDComm(
        io.iohk.atala.resolvers.UniversalDidResolver,
        MediatorSecretResolver.secretResolver
      ),
      Agent.Mediator
    )
  )
}
