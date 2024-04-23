package io.iohk.atala.oidc4vc.http

import sttp.tapir.Schema
import zio.json.*

final case class CreateCredentialConfigurationRequest(
    id: String,
    format: CredentialFormat,
    schemaId: String,
)

object CreateCredentialConfigurationRequest {
  given schema: Schema[CreateCredentialConfigurationRequest] = Schema.derived
  given encoder: JsonEncoder[CreateCredentialConfigurationRequest] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[CreateCredentialConfigurationRequest] = DeriveJsonDecoder.gen
}

final case class CredentialConfiguration(
    format: CredentialFormat,
    scope: String,
    credential_definition: CredentialDefinition,
    cryptographic_binding_methods_supported: Seq[String] = Seq("did:prism")
)

object CredentialConfiguration {
  given schema: Schema[CredentialConfiguration] = Schema.derived
  given encoder: JsonEncoder[CredentialConfiguration] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[CredentialConfiguration] = DeriveJsonDecoder.gen
}
