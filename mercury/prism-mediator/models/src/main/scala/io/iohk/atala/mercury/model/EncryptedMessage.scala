package io.iohk.atala.mercury.model

import org.didcommx.didcomm.model.PackEncryptedResult //FIXME REMOVE
import java.util.Base64

import io.circe._
import io.circe.parser._

case class EncryptedMessage(private val msg: PackEncryptedResult) {
  def string = msg.getPackedMessage
  def base64 = Base64.getUrlEncoder.encodeToString(string.getBytes)
  def asJson: JsonObject = {
    // TODO cleanup
    // println("#" * 120)
    // println(string)
    val aaa = parse(string).getOrElse(???) // .getOrElse(Json.Null)
    // println(aaa.spaces2)
    aaa.asObject.getOrElse(???) // TODO
  }
}
