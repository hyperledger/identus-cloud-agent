package org.hyperledger.identus.shared.utils

import org.erdtman.jcs.JsonCanonicalizer
import scala.util.Try

object Json {

  /** Canonicalizes a JSON string to JCS format according to RFC 8785
    *
    * @param jsonStr
    *   JSON string to canonicalize
    * @return
    *   canonicalized JSON string
    */

  def canonicalizeToJcs(jsonStr: String): Either[Throwable, String] = {
    val canonicalizer = Try { new JsonCanonicalizer(jsonStr) }
    canonicalizer.map(_.getEncodedString).toEither
  }
}
