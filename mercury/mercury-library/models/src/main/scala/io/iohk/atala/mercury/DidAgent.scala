package io.iohk.atala.mercury

import com.nimbusds.jose.jwk.*
import io.iohk.atala.mercury.model.DidId

/** Represente a Decentralized Identifier with secrets keys */
trait DidAgent {
  def id: DidId
  def jwkForKeyAgreement: Seq[OctetKeyPair]
  def jwkForKeyAuthentication: Seq[OctetKeyPair]
}
