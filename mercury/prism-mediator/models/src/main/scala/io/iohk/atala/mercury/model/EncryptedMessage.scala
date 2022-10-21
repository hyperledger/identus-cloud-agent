package io.iohk.atala.mercury.model

// import org.didcommx.didcomm.model.PackEncryptedResult //FIXME REMOVE
import java.util.Base64

import io.circe._
import io.circe.parser._

trait EncryptedMessage { // (private val msg: PackEncryptedResult) {
  def string: String // = msg.getPackedMessage
  def base64: String = Base64.getUrlEncoder.encodeToString(string.getBytes)
  def asJson: JsonObject = {
    val aux = parse(string).getOrElse(???) // .getOrElse(Json.Null)
    aux.asObject.getOrElse(???) // TODO
  }
}
