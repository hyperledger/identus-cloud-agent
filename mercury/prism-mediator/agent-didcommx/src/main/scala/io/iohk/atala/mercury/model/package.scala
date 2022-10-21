package io.iohk.atala.mercury.model

import org.didcommx.didcomm.model._

final case class EncryptedMessageImp(private val msg: PackEncryptedResult) extends EncryptedMessage {
  def string: String = msg.getPackedMessage
}

final case class SignedMessageImp(private val msg: PackSignedResult) extends SignedMesage {
  def string: String = msg.getPackedMessage
}

final case class UnpackMessageImp(private val msg: UnpackResult) extends UnpackMessage {
  def message: Message = {
    val aux = msg.getMessage
    aux
    ???
  }
}
