package io.iohk.atala.model

import org.didcommx.didcomm.model.PackEncryptedResult
import java.util.Base64

case class EncryptedMessage(private val msg: PackEncryptedResult) {
  def string = msg.getPackedMessage
  def base64 = Base64.getUrlEncoder.encodeToString(string.getBytes)
}

// TODO move you another module
given Conversion[PackEncryptedResult, EncryptedMessage] with {
  def apply(msg: PackEncryptedResult): EncryptedMessage =
    EncryptedMessage(msg)
}
