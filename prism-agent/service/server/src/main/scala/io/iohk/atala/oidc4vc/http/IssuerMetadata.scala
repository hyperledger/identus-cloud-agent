package io.iohk.atala.oidc4vc.http

import io.iohk.atala.pollux.core.model.oidc4vc as pollux
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.encodedName
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.net.URL

case class IssuerMetadata(
    credential_issuer: String,
    authorization_servers: Option[Seq[String]],
    credential_endpoint: String,
    credential_configurations_supported: Map[String, CredentialConfiguration]
)

object IssuerMetadata {
  given schema: Schema[IssuerMetadata] = Schema.derived
  given encoder: JsonEncoder[IssuerMetadata] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[IssuerMetadata] = DeriveJsonDecoder.gen

  def fromIssuer(issuer: pollux.CredentialIssuer, agentBaseUrl: URL): IssuerMetadata = {
    val credentialIssuerBaseUrl = agentBaseUrl.toURI().resolve(s"oidc4vc/issuers/${issuer.id}").toString
    IssuerMetadata(
      credential_issuer = credentialIssuerBaseUrl,
      authorization_servers = Some(Seq(issuer.authorizationServer.toString())),
      credential_endpoint = s"$credentialIssuerBaseUrl/credentials",
      credential_configurations_supported = Map(
        // FIXME: hardcode values
        "UniversityCredential" -> CredentialConfiguration(
          format = CredentialFormat.jwt_vc_json,
          scope = "UniversityCredential",
          credential_definition = CredentialDefinition(
            `@context` = Some(Seq("https://www.w3.org/2018/credentials/v1")),
            `type` = Seq("VerifiableCredential"),
            credentialSubject = None
          )
        )
      )
    )
  }
}
