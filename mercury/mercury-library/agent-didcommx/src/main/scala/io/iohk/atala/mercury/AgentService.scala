package io.iohk.atala.mercury

import org.didcommx.didcomm.DIDComm

import zio._
import org.didcommx.didcomm.model._

import io.iohk.atala.resolvers._
import io.iohk.atala.mercury.model.{_, given}
import java.util.Base64
import io.iohk.atala.mercury.DidComm

case class AgentService[A <: Agent](didComm: DIDComm, did: A) extends AgentServiceAny(didComm, did.id)

//TODO FIX THE NAME !!!
class AgentServiceAny(didComm: DIDComm, val myDid: DidId) extends DidComm {

  override def packSigned(msg: Message): UIO[SignedMesage] = {
    val params = new PackSignedParams.Builder(msg, myDid.value).build()
    ZIO.succeed(didComm.packSigned(params))
  }

  override def packEncrypted(msg: Message, to: DidId): UIO[EncryptedMessage] = {
    assert(msg.from == Some(myDid), s"ERROR in packEncrypted: ${msg.from} must be == to ${myDid}")
    val params = new PackEncryptedParams.Builder(msg, to.value)
      .from(myDid.value)
      .forward(false)
      .build()
    didComm.packEncrypted(params)
    ZIO.succeed(didComm.packEncrypted(params))
  }

  // override def packEncryptedForward(msg: Message, to: DidId): UIO[EncryptedMessage] = {
  //   assert(msg.from == Some(myDid), s"ERROR in packEncrypted: ${msg.from} must be == to ${myDid}")
  //   val params = new PackEncryptedParams.Builder(msg, to.value)
  //     .from(myDid.value)
  //     .forward(true)
  //     .build()
  //   didComm.packEncrypted(params)
  //   ZIO.succeed(didComm.packEncrypted(params))
  // }

  override def unpack(data: String): UIO[UnpackMessage] = {
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

  // val charlie = ZLayer.succeed(
  //   AgentService[Agent.Charlie.type](
  //     new DIDComm(UniversalDidResolver, CharlieSecretResolver.secretResolver),
  //     Agent.Charlie
  //   )
  // )

}
