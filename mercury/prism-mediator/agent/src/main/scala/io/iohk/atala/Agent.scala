package io.iohk.atala

import org.didcommx.didcomm.DIDComm

import zio._
import org.didcommx.didcomm.model._

import io.iohk.atala.resolvers._
import io.iohk.atala.model.{Message, SignedMesage, EncryptedMessage, UnpackMesage, given}
import java.util.Base64

enum Agent(val id: String):
  case Alice extends Agent("did:example:alice")
  case Bob extends Agent("did:example:bob")
  case Mediator extends Agent("did:example:mediator")

case class AgentService[A <: Agent](didComm: DIDComm, did: A) extends DIDCommService {

  override def packSigned(msg: Message): UIO[SignedMesage] = {
    val params = new PackSignedParams.Builder(msg, did.id).build()
    ZIO.succeed(didComm.packSigned(params))
  }

  override def packEncrypted(msg: Message, to: String): UIO[EncryptedMessage] = {
    val params = new PackEncryptedParams.Builder(msg, to).from(did.id).build()
    ZIO.succeed(didComm.packEncrypted(params))
  }

  override def unpack(data: String): UIO[UnpackMesage] = {
    ZIO.succeed(didComm.unpack(new UnpackParams.Builder(data).build()))
  }

  override def unpackBase64(dataBase64: String): UIO[UnpackMesage] = {
    val data = new String(Base64.getUrlDecoder.decode(dataBase64))
    ZIO.succeed(didComm.unpack(new UnpackParams.Builder(data).build()))
  }

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
  val zzz = AgentService[Agent.Mediator.type](
    new DIDComm(UniversalDidResolver, MediatorSecretResolver.secretResolver),
    Agent.Mediator
  )
}
