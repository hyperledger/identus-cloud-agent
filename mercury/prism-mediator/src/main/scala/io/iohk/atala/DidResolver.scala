package io.iohk.atala

object DidResolver {
  def lookupDid(did: String): Option[DidDocument] = {
    Some(DidDocument("did:prism:testnet:12345"))
  }

  case class DidDocument(id: String)
}
