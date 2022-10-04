package io.iohk.atala.castor.core.model.did

final case class DID(
    method: String,
    methodSpecificId: String
) {
  override def toString: String = s"did:$method:$methodSpecificId"
}
