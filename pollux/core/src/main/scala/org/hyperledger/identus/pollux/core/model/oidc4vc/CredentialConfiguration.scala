package org.hyperledger.identus.pollux.core.model.oidc4vc

import org.hyperledger.identus.pollux.core.model.CredentialFormat
import zio.json.ast.Json

import java.net.URI

final case class CredentialConfiguration(
    configurationId: String,
    format: CredentialFormat,
    schemaId: URI,
    dereferencedSchema: Json
) {
  def scope: String = configurationId
}
