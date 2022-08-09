package io.iohk.atala.model

import org.didcommx.didcomm.model.UnpackResult

case class UnpackMesage(private val msg: UnpackResult) {
  def message = msg.getMessage
  def getMessage = msg.getMessage() // REMOVE TODO
}

// TODO move you another module
given Conversion[UnpackResult, UnpackMesage] with {
  def apply(msg: UnpackResult): UnpackMesage =
    UnpackMesage(msg)
}

// given Conversion[UnpackMesage, UnpackResult] with {
//   def apply(msg: UnpackMesage): UnpackResult = msg.msg
// }
