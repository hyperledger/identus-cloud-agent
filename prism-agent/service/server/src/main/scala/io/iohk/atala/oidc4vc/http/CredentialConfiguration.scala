package io.iohk.atala.oidc4vc.http

import io.iohk.atala.pollux.core.model.oidc4vc.CredentialConfiguration as PolluxCredentialConfiguration
import sttp.tapir.Schema
import zio.json.*

import scala.language.implicitConversions

final case class CreateCredentialConfigurationRequest(
    configurationId: String,
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

  given Conversion[PolluxCredentialConfiguration, CredentialConfiguration] = cc =>
    CredentialConfiguration(
      format = cc.format,
      scope = cc.scope,
      credential_definition = CredentialDefinition(
        `@context` = Some(Seq("https://www.w3.org/2018/credentials/v1")),
        `type` = Seq("VerifiableCredential"),
        credentialSubject = Some(Map.empty) // TODO: implement conversion from JsonSchhema
      )
    )
}
