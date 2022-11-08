package io.iohk.atala.mercury.model

import java.io.IOException

package object error {

  type MercuryException = MercuryError | IOException
  type MercuryThrowable = MercuryError | IOException | Throwable // REMOVE Throwable

  def mercuryErrorAsThrowable(error: MercuryThrowable): java.lang.Throwable = error match
    // case ex: MercuryError =>
    //   ex match
    //     case te: TransportError => new RuntimeException(te)
    case ex: IOException => ex
    case ex: Throwable   => ex

  sealed trait MercuryError

  trait TransportError extends Exception with MercuryError

  case class SendMessage(cause: Throwable)
      extends RuntimeException("Error when sending message", cause)
      with TransportError

  case class ParseResponse(cause: Throwable)
      extends RuntimeException("Error when parsing response", cause)
      with TransportError
}
