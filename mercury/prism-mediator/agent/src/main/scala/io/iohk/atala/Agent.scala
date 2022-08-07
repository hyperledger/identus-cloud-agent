package io.iohk.atala

import org.didcommx.didcomm.DIDComm
import org.didcommx.didcomm.message.Message

import zio._
import org.didcommx.didcomm.model._
import io.iohk.atala.resolvers._

enum Agent(val id: String):
  case Alice extends Agent("did:example:alice")
  case Bob extends Agent("did:example:bob")
  case Mediator extends Agent("did:example:mediator")

case class AgentService[A <: Agent](didComm: DIDComm, did: A) extends DIDCommService {

  override def packSigned(msg: Message): UIO[PackSignedResult] = {
    val params = new PackSignedParams.Builder(msg, did.id).build()
    ZIO.succeed(didComm.packSigned(params))
  }

  override def packEncrypted(msg: Message, to: String): UIO[PackEncryptedResult] = {
    val params = new PackEncryptedParams.Builder(msg, to).from(did.id).build()
    ZIO.succeed(didComm.packEncrypted(params))
  }

  override def unpack(base64str: String): UIO[UnpackResult] =
    ZIO.succeed(didComm.unpack(new UnpackParams.Builder(base64str).build()))

}

object AgentService {
  val alice = ZLayer.succeed(
    AgentService[Agent.Alice.type](
      new DIDComm(UniversalDidResolver, AliceSecretResolver.secretResolver),
      Agent.Alice
    )
  )
  val bob = ZLayer.succeed(
    AgentService[Agent.Bob.type](
      new DIDComm(UniversalDidResolver, BobSecretResolver.secretResolver),
      Agent.Bob
    )
  )
  val mediator = ZLayer.succeed(
    AgentService[Agent.Mediator.type](
      new DIDComm(UniversalDidResolver, MediatorSecretResolver.secretResolver),
      Agent.Mediator
    )
  )
}
