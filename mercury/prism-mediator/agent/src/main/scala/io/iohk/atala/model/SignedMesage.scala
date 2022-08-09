package io.iohk.atala.model

import org.didcommx.didcomm.model.PackSignedResult
import java.util.Base64

case class SignedMesage(private val msg: PackSignedResult) {
  def string = msg.getPackedMessage
  def base64 = Base64.getUrlEncoder.encodeToString(string.getBytes)
}

// TODO move you another module
given Conversion[PackSignedResult, SignedMesage] with {
  def apply(msg: PackSignedResult): SignedMesage =
    SignedMesage(msg)
}
