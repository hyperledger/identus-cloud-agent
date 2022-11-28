package io.iohk.atala.pollux.vc.jwt.schema

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.iohk.atala.pollux.vc.jwt.CredentialSchema
import zio.IO
import zio.prelude.Validation

trait SchemaResolver {
  def resolve(credentialSchema: CredentialSchema): IO[String, Json]
}
