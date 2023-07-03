package io.iohk.atala.castor.core.model.did

import io.iohk.atala.prism.identity as prismIdentity

import scala.util.Try

final case class DIDMethod private[did] (value: String) {
  override def toString: String = value
}

object DIDMethod {
  def fromString(s: String): Try[DIDMethod] = {
    Try(DIDMethod(prismIdentity.DidMethod.Companion.fromString(s).toString))
  }
}

final case class DIDMethodSpecificId private[did] (value: String) {
  override def toString: String = value
}

object DIDMethodSpecificId {
  def fromString(s: String): Try[DIDMethodSpecificId] = {
    Try(DIDMethodSpecificId(prismIdentity.DidMethodSpecificId.Companion.fromString(s).toString))
  }
}

final case class DID(
    method: DIDMethod,
    methodSpecificId: DIDMethodSpecificId
) {
  override def toString: String = s"did:${method.value}:${methodSpecificId.value}"
}

object DID {
  // TODO: implement
  def fromString(s: String): Either[String, DID] = ???
}
