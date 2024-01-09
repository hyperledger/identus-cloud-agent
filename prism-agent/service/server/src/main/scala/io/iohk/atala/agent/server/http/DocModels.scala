package io.iohk.atala.agent.server.http

import io.iohk.atala.connect.controller.ConnectionEndpoints
import io.iohk.atala.pollux.credentialschema.VerificationPolicyEndpoints
import sttp.apispec.openapi.*
import sttp.apispec.{SecurityScheme, Tag}
import sttp.model.headers.AuthenticationScheme

import scala.collection.immutable.ListMap

object DocModels {

  private val apiKeySecuritySchema = SecurityScheme(
    `type` = "apiKey",
    description = Some("API Key Authentication. The header `apikey` must be set with the API key."),
    name = Some("apikey"),
    in = Some("header"),
    scheme = None,
    bearerFormat = None,
    flows = None,
    openIdConnectUrl = None
  )

  private val adminApiKeySecuritySchema = SecurityScheme(
    `type` = "apiKey",
    description =
      Some("Admin API Key Authentication. The header `x-admin-api-key` must be set with the Admin API key."),
    name = Some("x-admin-api-key"),
    in = Some("header"),
    scheme = None,
    bearerFormat = None,
    flows = None,
    openIdConnectUrl = None
  )

  private val jwtSecurityScheme = SecurityScheme(
    `type` = "http",
    description =
      Some("JWT Authentication. The header `Authorization` must be set with the JWT token using `Bearer` scheme"),
    name = Some("Authorization"),
    in = Some("header"),
    scheme = Some(AuthenticationScheme.Bearer.name),
    bearerFormat = None,
    flows = None,
    openIdConnectUrl = None
  )

  val customiseDocsModel: OpenAPI => OpenAPI = { oapi =>
    oapi
      .info(
        Info(
          title = "Open Enterprise Agent API Reference",
          version = "1.0", // Will be replaced dynamically by 'Tapir2StaticOAS'
          summary = Some("""
              |This API provides interfaces for managing decentralized identities and secure communications in a self-sovereign identity framework.
              |It enables seamless interaction with various decentralized identity protocols and services using the [Open Enterprise Agent](https://github.com/hyperledger-labs/open-enterprise-agent)
              |""".stripMargin),
          description = Some("""
              |The Open Enterprise Agent API facilitates the integration and management of self-sovereign identity capabilities within applications.
              |It supports DID (Decentralized Identifiers) management, verifiable credential exchange, and secure messaging based on DIDComm standards.
              |The API is designed to be interoperable with various blockchain and DLT (Distributed Ledger Technology) platforms, ensuring wide compatibility and flexibility.
              |Key features include connection management, credential issuance and verification, and secure, privacy-preserving communication between entities.
              |Additional information and the full list of capabilities can be found in the [Open Enterprise Agent documentation](https://docs.atalaprism.io/docs/category/prism-cloud-agent)
              |""".stripMargin),
          termsOfService = Some("""
              |Users of the Open Enterprise Agent API must adhere to the terms and conditions outlined in [Link to Terms of Service](/).
              |This includes compliance with relevant data protection regulations, responsible usage policies, and adherence to the principles of decentralized identity management.
              |""".stripMargin),
          contact = Some(
            Contact(
              name = Some("Contact - Name"),
              email = Some("Contact - Email"),
              url = Some("Contact - URL"),
              extensions = ListMap.empty
            )
          ),
          license = Some(
            License(
              name = "Apache 2.0",
              url = Some("https://www.apache.org/licenses/LICENSE-2.0"),
              extensions = ListMap.empty
            )
          ),
          extensions = ListMap.empty
        )
      )
      .servers(
        List(
          Server(url = "http://localhost:8085", description = Some("Local Prism Agent")),
          Server(url = "http://localhost/prism-agent", description = Some("Local Prism Agent with APISIX proxy")),
          Server(
            url = "https://k8s-dev.atalaprism.io/prism-agent",
            description = Some("Prism Agent on the Staging Environment")
          ),
        )
      )
      .components(
        oapi.components
          .getOrElse(sttp.apispec.openapi.Components.Empty)
          .copy(securitySchemes =
            ListMap(
              "apiKeyAuth" -> Right(apiKeySecuritySchema),
              "adminApiKeyAuth" -> Right(adminApiKeySecuritySchema),
              "jwtAuth" -> Right(jwtSecurityScheme)
            )
          )
      )
      .addSecurity(
        ListMap(
          "apiKeyAuth" -> Vector.empty[String],
          "adminApiKeyAuth" -> Vector.empty[String],
          "jwtAuth" -> Vector.empty[String]
        )
      )
      .tags(
        List(
          ConnectionEndpoints.tag,
          VerificationPolicyEndpoints.tag
        )
      )

  }

}
