package io.iohk.atala.castor.core.model.did

final case class DIDUrl(
    did: DID,
    path: Seq[String],
    parameters: Map[String, Seq[String]],
    fragment: Option[String]
)
