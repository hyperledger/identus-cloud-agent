package io.iohk.atala.mercury

import org.didcommx.didcomm.model._
import org.didcommx.didcomm.message.MessageBuilder

import scala.jdk.CollectionConverters._

import io.iohk.atala.mercury.model._

// TODO move you another module
given Conversion[PackEncryptedResult, EncryptedMessage] with {
  def apply(msg: PackEncryptedResult): EncryptedMessage =
    EncryptedMessage(msg)
}

// TODO move you another module
given Conversion[Message, org.didcommx.didcomm.message.Message] with {
  def apply(msg: Message): org.didcommx.didcomm.message.Message =
    new MessageBuilder(msg.id, msg.body.asJava, msg.piuri)
      .from(msg.from.value)
      .to(Seq(msg.to.value).asJava)
      .createdTime(msg.createdTime)
      .expiresTime(msg.createdTime + msg.expiresTimePlus)
      .build()
}

// TODO move you another module
given Conversion[PackSignedResult, SignedMesage] with {
  def apply(msg: PackSignedResult): SignedMesage =
    SignedMesage(msg)
}

// TODO move you another module
given Conversion[UnpackResult, UnpackMesage] with {
  def apply(msg: UnpackResult): UnpackMesage =
    UnpackMesage(msg)
}

// given Conversion[UnpackMesage, UnpackResult] with {
//   def apply(msg: UnpackMesage): UnpackResult = msg.msg
// }


