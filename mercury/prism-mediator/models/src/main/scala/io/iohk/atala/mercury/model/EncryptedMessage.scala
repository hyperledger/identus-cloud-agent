package io.iohk.atala.mercury.model

import org.didcommx.didcomm.model.PackEncryptedResult //FIXME REMOVE
import java.util.Base64

case class EncryptedMessage(private val msg: PackEncryptedResult) {
  def string = msg.getPackedMessage
  def base64 = Base64.getUrlEncoder.encodeToString(string.getBytes)
}
