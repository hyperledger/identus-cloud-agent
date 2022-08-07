package io.iohk.atala

import io.iohk.atala.resolvers.{
  AliceSecretResolver,
  BobSecretResolver,
  MediatorSecretResolver,
  UniversalDidResolver
}
import org.didcommx.didcomm.DIDComm
import org.didcommx.didcomm.message.Attachment.Data.{Companion, Json}
import org.didcommx.didcomm.message.{Attachment, MessageBuilder}
import org.didcommx.didcomm.model.PackEncryptedParams
import org.didcommx.didcomm.model.UnpackParams
import org.didcommx.didcomm.protocols.routing.Routing

import java.time.{LocalDateTime, ZoneOffset}
import scala.jdk.CollectionConverters._
import org.didcommx.didcomm.utils.JSONUtilsKt.toJson

import java.util
import java.util.Base64

object DIDCommPlay {

  def run(): Unit = {
    val didComm =
      new DIDComm(UniversalDidResolver, AliceSecretResolver.secretResolver)
    val didCommMediator =
      new Routing(UniversalDidResolver, BobSecretResolver.secretResolver)

    val id = "1234567890"
    val connectionId = "8fb9ea21-d094-4506-86b6-c7c1627d753a"
    val msg = "Hello Bob"
    val body = Map("connectionId" -> connectionId, "msg" -> msg).asJava
    val `type` = "http://atalaprism.io/lets_connect/proposal"
    val ALICE_DID = "did:example:alice"
    val BOB_DID = "did:example:bob"
    val createdTime = LocalDateTime.now().toEpochSecond(ZoneOffset.of("Z"))
    val expiresTime = createdTime + 1000
    val message = new MessageBuilder(id, body, `type`)
    message.from(ALICE_DID)
    message.to(Seq(BOB_DID).asJava)
    message.createdTime(createdTime)
    message.expiresTime(expiresTime)

    val messageCreated = message.build()

    val buildPackForBob =
      new PackEncryptedParams.Builder(messageCreated, BOB_DID)
    val packResult =
      didComm.packEncrypted(buildPackForBob.from(ALICE_DID).build())

    println(
      s"**************************************************************************************************************************"
    )
    println(
      s"Sending ${packResult.getPackedMessage} to ${Option(packResult.getServiceMetadata)
          .map(_.getServiceEndpoint)}"
    )
    val base64EncodedString =
      Base64.getUrlEncoder.encodeToString(packResult.getPackedMessage.getBytes)
    println(s"Base64EncodedString \n${base64EncodedString}\n")
    println(
      s"Base64DecodedString \n${new String(Base64.getUrlDecoder.decode(base64EncodedString))}\n"
    )

    println(
      s"**************************************************************************************************************************"
    )

//    val unpackResult = didComm.unpack(
//      new UnpackParams.Builder(packResult.getPackedMessage)
//        .secretResolver(MediatorSecretResolver.secretResolver)
//        .build()
//    )
//
//    println(s"**************************************************************************************************************************")
//    println(s"\nGot ${unpackResult.getMessage} message\n")
//    println(s"**************************************************************************************************************************")

    // BOB MEDIATOR
    val forwardBob = didCommMediator.unpackForward(
      packResult.getPackedMessage,
      true,
      UniversalDidResolver,
      MediatorSecretResolver.secretResolver
    )

    val forwardedMsg = toJson(forwardBob.getForwardMsg.getMessage)
    println(
      s"**************************************************************************************************************************"
    )
    println(s"BOB MEDIATOR \n ${forwardedMsg} \n")
    println(
      s"**************************************************************************************************************************"
    )

    // BOB
    val unpackResult1 = didComm.unpack(
      new UnpackParams.Builder(forwardedMsg)
        .secretResolver(BobSecretResolver.secretResolver)
        .build()
    )
    println(
      s"**************************************************************************************************************************"
    )
    println(s"Got forward  mediator \n ${unpackResult1.getMessage} \n message")
    println(
      s"**************************************************************************************************************************"
    )

  }
}
