package io.iohk.atala.agent.server.http

import io.iohk.atala.connect.controller.ConnectionEndpoints
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
          summary = Some("Info - Summary"),
          description = Some("Info - Description"),
          termsOfService = Some("Info - Terms Of Service"),
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
              name = "License - Name",
              url = Some("License - URL"),
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
          Tag(
            ConnectionEndpoints.TAG,
            Some(
              s"""
                 |The '${ConnectionEndpoints.TAG}' endpoints facilitate the initiation of connection flows between the current agent and peer agents, regardless of whether they reside in cloud or edge environments.
                 |<br>
                 |This implementation adheres to the DIDComm Messaging v2.0 - [Out of Band Messages](https://identity.foundation/didcomm-messaging/spec/v2.0/#out-of-band-messages) specification [section 9.5.4](https://identity.foundation/didcomm-messaging/spec/v2.0/#invitation) - to generate invitations.
                 |The <b>from</b> field of the out-of-band invitation message contains a freshly generated Peer DID that complies with the [did:peer:2](https://identity.foundation/peer-did-method-spec/#generating-a-didpeer2) specification.
                 |This Peer DID includes the 'uri' location of the DIDComm messaging service, essential for the invitee's subsequent execution of the connection flow.
                 |<br>
                 |Upon accepting an invitation, the invitee sends a connection request to the inviter's DIDComm messaging service endpoint.
                 |The connection request's 'type' attribute must be specified as "https://atalaprism.io/mercury/connections/1.0/request".
                 |The inviter agent responds with a connection response message, indicated by a 'type' attribute of "https://atalaprism.io/mercury/connections/1.0/response".
                 |Both request and response types are proprietary to the Open Enterprise Agent ecosystem.
                 |""".stripMargin
            )
          )
        )
      )

  }

}
