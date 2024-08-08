package org.hyperledger.identus.mercury.model

import io.circe.*
import io.circe.parser.*

import java.util.Base64

trait EncryptedMessage { // (private val msg: PackEncryptedResult) {
  def string: String // = msg.getPackedMessage
  def base64: String = Base64.getUrlEncoder.encodeToString(string.getBytes)
  def asJson: JsonObject = {
    parse(string)
      .flatMap(o =>
        o.asObject match
          case None =>
            Left(
              ParsingFailure(
                "Expecting the Json to be a Json Object",
                RuntimeException(s"Expecting Json Object got '$o'")
              )
            )
          case Some(value) => Right(value)
      )
      .getOrElse(UnexpectedCodeExecutionPath)
  }
}
