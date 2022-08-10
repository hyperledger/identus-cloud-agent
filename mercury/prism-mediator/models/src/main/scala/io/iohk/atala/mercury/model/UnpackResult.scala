package io.iohk.atala.mercury.model

import org.didcommx.didcomm.model.UnpackResult //FIXME REMOVE

case class UnpackMesage(private val msg: UnpackResult) {
  def message = msg.getMessage
  def getMessage = msg.getMessage() // REMOVE TODO
}
