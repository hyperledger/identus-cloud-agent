package io.iohk.atala

import io.iohk.atala.resolvers.{AliceSecretResolver, BobSecretResolver, UniversalDidResolver}
import org.didcommx.didcomm.DIDComm
import org.didcommx.didcomm.message.MessageBuilder
import org.didcommx.didcomm.model.PackEncryptedParams.Builder
import org.didcommx.didcomm.model.UnpackParams

import java.time.{LocalDateTime, ZoneOffset}
import scala.jdk.CollectionConverters._

object DIDCommPlay {

  def run(): Unit = {
    val didComm = new DIDComm(UniversalDidResolver, AliceSecretResolver.secretResolver)
    val id = "1234567890"
    val body = Map("messagespecificattribute" -> "and its value").asJava
    val `type` = "http://atalaprism.io/lets_connect/proposal"
    val message = new MessageBuilder(id, body, `type`)
    val ALICE_DID = "did:example:alice"
    val BOB_DID = "did:example:bob"
    val createdTime = LocalDateTime.now().toEpochSecond(ZoneOffset.of("Z"))
    val expiresTime = createdTime + 1000

    message.from(ALICE_DID)
    message.to(Seq(BOB_DID).asJava)
    message.createdTime(createdTime)
    message.expiresTime(expiresTime)
    val xxx = message.build()

    val xx = new Builder(xxx, BOB_DID)
    val packResult = didComm.packEncrypted(
      xx
        .from(ALICE_DID)
        .build()
    )
    println(s"**************************************************************************************************************************")
    println(s"Sending ${packResult.getPackedMessage} to ${Option(packResult.getServiceMetadata).map(_.getServiceEndpoint)}")
    println(s"**************************************************************************************************************************")

    val unpackResult = didComm.unpack(
      new UnpackParams.Builder(packResult.getPackedMessage)
        .secretResolver(BobSecretResolver.secretResolver)
        .build()
    )

    println(s"**************************************************************************************************************************")
    println(s"\nGot ${unpackResult.getMessage} message\n")
    println(s"**************************************************************************************************************************")
  }
}
