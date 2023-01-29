package io.iohk.atala.mercury.resolvers

import zio._
import io.iohk.atala.mercury._
import org.didcommx.didcomm.DIDComm

object MediatorDidComm {

  /** This genereate a new Agent with new keys each time it start.
    *
    * The identity of mediator will need to be static. TODO
    */
  val peerDidMediator: ZLayer[Any, Nothing, DidAgent] =
    AgentPeerService.makeLayer(
      PeerDidMediatorSecretResolver.peer
    )

}
