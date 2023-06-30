package io.iohk.atala.pollux.vc.jwt.schema

import io.circe.Json
import io.iohk.atala.pollux.vc.jwt.CredentialSchema
import zio.IO

trait SchemaResolver {
  def resolve(credentialSchema: CredentialSchema): IO[String, Json]
}
