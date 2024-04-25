package org.hyperledger.identus.pollux.vc.jwt.schema

import io.circe.Json
import org.hyperledger.identus.pollux.vc.jwt.CredentialSchema
import zio.IO

trait SchemaResolver {
  def resolve(credentialSchema: CredentialSchema): IO[String, Json]
}
