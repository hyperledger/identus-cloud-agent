package org.hyperledger.identus.shared.utils

import org.erdtman.jcs.JsonCanonicalizer

import java.io.IOException

object Json {

  /** Canonicalizes a JSON string to JCS format according to RFC 8785
    *
    * @param jsonStr
    *   JSON string to canonicalize
    * @return
    *   canonicalized JSON string
    */

  def canonicalizeToJcs(jsonStr: String): Either[IOException, String] =
    try { Right(new JsonCanonicalizer(jsonStr).getEncodedString) }
    catch case exception: IOException => Left(exception)
}
