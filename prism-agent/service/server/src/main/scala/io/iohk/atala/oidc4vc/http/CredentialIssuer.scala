package io.iohk.atala.oidc4vc.http

import io.iohk.atala.pollux.core.model.oidc4vc.CredentialIssuer as PolluxCredentialIssuer
import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.util.UUID

case class CreateCredentialIssuerRequest(authorizationServer: String)

object CreateCredentialIssuerRequest {
  given schema: Schema[CreateCredentialIssuerRequest] = Schema.derived
  given encoder: JsonEncoder[CreateCredentialIssuerRequest] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[CreateCredentialIssuerRequest] = DeriveJsonDecoder.gen
}

case class CredentialIssuer(id: UUID, authorizationServer: String)

object CredentialIssuer {
  given schema: Schema[CredentialIssuer] = Schema.derived
  given encoder: JsonEncoder[CredentialIssuer] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[CredentialIssuer] = DeriveJsonDecoder.gen

  given Conversion[PolluxCredentialIssuer, CredentialIssuer] = domain =>
    CredentialIssuer(domain.id, domain.authorizationServer.toString)
}
