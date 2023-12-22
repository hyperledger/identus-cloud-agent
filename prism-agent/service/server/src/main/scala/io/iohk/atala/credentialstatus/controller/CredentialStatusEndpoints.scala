package io.iohk.atala.credentialstatus.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.credentialstatus.controller.http.CredentialStatusList
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import java.util.UUID

object CredentialStatusEndpoints {

  val getCredentialStatusListEndpoint: PublicEndpoint[
    (RequestContext, UUID),
    ErrorResponse,
    CredentialStatusList,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "credential-status" / path[UUID]("id").description(
          "Globally unique identifier of the credential status list"
        )
      )
      .out(jsonBody[CredentialStatusList].description("Credential status list found by ID"))
      .errorOut(basicFailuresAndNotFound)
      .name("getCredentialStatusListEndpoint")
      .summary("Fetch credential status list by its ID")
      .description(
        "Fetch credential status list by its ID"
      )
      .tag("Credential status list")
}
