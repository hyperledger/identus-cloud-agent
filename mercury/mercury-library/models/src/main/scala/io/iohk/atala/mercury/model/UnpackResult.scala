package io.iohk.atala.mercury.model

trait UnpackMessage {
  def message: Message
  def getMessage: Message = message // REMOVE TODO
}
