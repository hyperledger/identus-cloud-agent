package org.hyperledger.identus.mercury.model

package object error {

  sealed trait MercuryError

  trait TransportError extends MercuryError

  sealed case class SendMessageError(cause: Throwable, mData: Option[String] = None) extends TransportError

}
