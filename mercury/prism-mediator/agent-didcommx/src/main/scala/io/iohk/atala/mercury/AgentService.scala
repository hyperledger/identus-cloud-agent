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
      .didDocResolver(UniversalDidResolver)
      .secretResolver(CharlieSecretResolver.secretResolver)
      .build()
    println("111111")
    println(params)

    // PackEncryptedParams(message={"id":"683cd581-896e-430f-a4d8-8bc1b6f940e3",
    // "typ":"application\/didcomm-plain+json",
    // "type":"https:\/\/didcomm.org\/coordinate-mediation\/2.0\/mediate-request",
    // "from":"did:peer:2.Ez6LSiseNCbbtmG6ascxpPvoyT8ewrWdtJZxwmPNxYAPWxzM8.Vz6MkgLBGee6xL5KH8SZmqmKmQKS2o1qd4RG4dSmjtRGTfsxX.SW3sidCI6ImRtIiwicyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODAwMC8iLCJyIjpbImRpZDpleGFtcGxlOnNvbWVtZWRpYXRvciNzb21la2V5Il19LHsidCI6ImV4YW1wbGUiLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDAwLyIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkyIl0sImEiOlsiZGlkY29tbS92MiIsImRpZGNvbW0vYWlwMjtlbnY9cmZjNTg3Il19XQ",
    // "to":["did:peer:2.Ez6LSdcC5vAY6BZ56v2CZbqTknMQwtxZHYN4P2jniKqFsv2gH.Vz6Mkgssis6KGWTyKRZNJbzqRDQvd6wWYfb69Vj1fbibRxmQ6.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwOi8vcm9vdHNpZC1tZWRpYXRvcjo4MDAwIiwiYSI6WyJkaWRjb21tL3YyIl19"]
    // ,"created_time":1664454893,"expires_time":1664455893,"body":{},"attachments":[]},
    //  to=did:peer:2.Ez6LSdcC5vAY6BZ56v2CZbqTknMQwtxZHYN4P2jniKqFsv2gH.Vz6Mkgssis6KGWTyKRZNJbzqRDQvd6wWYfb69Vj1fbibRxmQ6.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwOi8vcm9vdHNpZC1tZWRpYXRvcjo4MDAwIiwiYSI6WyJkaWRjb21tL3YyIl19,
    //   from=did:peer:2.Ez6LSiseNCbbtmG6ascxpPvoyT8ewrWdtJZxwmPNxYAPWxzM8.Vz6MkgLBGee6xL5KH8SZmqmKmQKS2o1qd4RG4dSmjtRGTfsxX.SW3sidCI6ImRtIiwicyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODAwMC8iLCJyIjpbImRpZDpleGFtcGxlOnNvbWVtZWRpYXRvciNzb21la2V5Il19LHsidCI6ImV4YW1wbGUiLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDAwLyIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkyIl0sImEiOlsiZGlkY29tbS92MiIsImRpZGNvbW0vYWlwMjtlbnY9cmZjNTg3Il19XQ,
    //   signFrom=null, fromPriorIssuerKid=null, encAlgAuth=A256CBC_HS512_ECDH_1PU_A256KW, encAlgAnon=XC20P_ECDH_ES_A256KW, protectSenderId=false, forward=false, forwardHeaders=null, forwardServiceId=null,
    //    didDocResolver=null, secretResolver=null)

    didComm.packEncrypted(params)
    println("222222222")

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

  val charlie = ZLayer.succeed(
    AgentService[Agent.Charlie.type](
      new DIDComm(UniversalDidResolver, CharlieSecretResolver.secretResolver),
      Agent.Charlie
    )
  )

}
