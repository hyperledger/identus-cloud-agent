package io.iohk.atala.mercury.model

import org.didcommx.didcomm.model.PackSignedResult //FIXME REMOVE
import java.util.Base64

case class SignedMesage(private val msg: PackSignedResult) {
  def string = msg.getPackedMessage
  def base64 = Base64.getUrlEncoder.encodeToString(string.getBytes)
}
