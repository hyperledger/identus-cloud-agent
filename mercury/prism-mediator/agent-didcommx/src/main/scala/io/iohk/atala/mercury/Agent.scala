package io.iohk.atala.mercury

import org.didcommx.didcomm.DIDComm

import zio._
import org.didcommx.didcomm.model._

import io.iohk.atala.resolvers._
import io.iohk.atala.mercury.model.{_, given}
import java.util.Base64
import io.iohk.atala.mercury.DidComm

case class AgentService[A <: Agent](didComm: DIDComm, did: A) extends DidComm {

  override def packSigned(msg: Message): UIO[SignedMesage] = {
    val params = new PackSignedParams.Builder(msg, did.id.value).build()
    ZIO.succeed(didComm.packSigned(params))
  }

  override def packEncrypted(msg: Message, to: DidId): UIO[EncryptedMessage] = {
    val params = new PackEncryptedParams.Builder(msg, to.value)
      .from(did.id.value)
      .forward(false)
      .build()
    ZIO.succeed(didComm.packEncrypted(params))
  }

  override def unpack(data: String): UIO[UnpackMesage] = {
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

}
