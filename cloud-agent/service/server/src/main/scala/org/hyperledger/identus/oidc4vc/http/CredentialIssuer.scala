package org.hyperledger.identus.oidc4vc.http

import org.hyperledger.identus.pollux.core.model.oidc4vc.CredentialIssuer as PolluxCredentialIssuer
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

case class CredentialIssuerPage(
    self: String,
    kind: String = "CredentialIssuerPage",
    pageOf: String,
    next: Option[String] = None,
    previous: Option[String] = None,
    contents: Seq[CredentialIssuer]
)

object CredentialIssuerPage {
  given schema: Schema[CredentialIssuerPage] = Schema.derived
  given encoder: JsonEncoder[CredentialIssuerPage] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[CredentialIssuerPage] = DeriveJsonDecoder.gen
}
