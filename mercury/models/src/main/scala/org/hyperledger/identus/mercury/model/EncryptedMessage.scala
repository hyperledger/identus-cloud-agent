package org.hyperledger.identus.mercury.model

import io.circe.*
import io.circe.parser.*

import java.util.Base64

trait EncryptedMessage { // (private val msg: PackEncryptedResult) {
  def string: String // = msg.getPackedMessage
  def base64: String = Base64.getUrlEncoder.encodeToString(string.getBytes)
  def asJson: JsonObject = {
    val aux = parse(string).getOrElse(???) // .getOrElse(Json.Null)
    aux.asObject.getOrElse(???) // TODO
  }
}
