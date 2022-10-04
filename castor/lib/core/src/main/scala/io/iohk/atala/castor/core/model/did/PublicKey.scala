package io.iohk.atala.castor.core.model.did

sealed abstract class PublicKey {
  val id: String
  val purposes: Seq[VerificationRelationship]
}

object PublicKey {
  final case class JsonWebKey2020(
      id: String,
      purposes: Seq[VerificationRelationship],
      publicKeyJwk: PublicKeyJwk
  ) extends PublicKey
}
