package io.iohk.atala.pollux.server.http.marshaller

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.iohk.atala.pollux.openapi.model.*
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  given RootJsonFormat[CreateCredentials201Response] = jsonFormat3(
    CreateCredentials201Response.apply
  )

  given RootJsonFormat[CreateCredentialsRequest] = jsonFormat2(
    CreateCredentialsRequest.apply
  )

  given RootJsonFormat[RevocationStatus] = jsonFormat2(
    RevocationStatus.apply
  )

  given RootJsonFormat[W3CCredential] = jsonFormat6(
    W3CCredential.apply
  )

  given RootJsonFormat[W3CCredentialCredentialSubject] = jsonFormat1(
    W3CCredentialCredentialSubject.apply
  )

  given RootJsonFormat[W3CCredentialInput] = jsonFormat3(
    W3CCredentialInput.apply
  )

  given RootJsonFormat[W3CCredentialInputClaims] = jsonFormat1(
    W3CCredentialInputClaims.apply
  )

  given RootJsonFormat[W3CCredentialRevocationRequest] = jsonFormat1(
    W3CCredentialRevocationRequest.apply
  )

  given RootJsonFormat[W3CCredentialRevocationResponse] = jsonFormat2(
    W3CCredentialRevocationResponse.apply
  )

  given RootJsonFormat[W3CCredentialsPaginated] = jsonFormat4(
    W3CCredentialsPaginated.apply
  )

  given RootJsonFormat[W3CCredentialStatus] = jsonFormat2(
    W3CCredentialStatus.apply
  )

  given RootJsonFormat[W3CIssuanceBatch] = jsonFormat3(
    W3CIssuanceBatch.apply
  )

  given RootJsonFormat[W3CIssuanceBatchAction] = jsonFormat3(
    W3CIssuanceBatchAction.apply
  )

  given RootJsonFormat[W3CIssuanceBatchPaginated] = jsonFormat4(
    W3CIssuanceBatchPaginated.apply
  )

  given RootJsonFormat[W3CPresentation] = jsonFormat1(
    W3CPresentation.apply
  )

  given RootJsonFormat[W3CPresentationInput] = jsonFormat1(
    W3CPresentationInput.apply
  )

  given RootJsonFormat[W3CPresentationPaginated] = jsonFormat4(
    W3CPresentationPaginated.apply
  )

  given RootJsonFormat[W3CProof] = jsonFormat6(
    W3CProof.apply
  )

  given RootJsonFormat[W3CSchema] = jsonFormat8(
    W3CSchema.apply
  )

  given RootJsonFormat[W3CSchemaAllOf] = jsonFormat2(
    W3CSchemaAllOf.apply
  )

  given RootJsonFormat[W3CSchemaClaims] = jsonFormat7(
    W3CSchemaClaims.apply
  )

  given RootJsonFormat[W3CSchemaClaimsProperties] = jsonFormat1(
    W3CSchemaClaimsProperties.apply
  )

  given RootJsonFormat[W3CSchemaInput] = jsonFormat4(
    W3CSchemaInput.apply
  )

  given RootJsonFormat[W3CSchemaMeta] = jsonFormat6(
    W3CSchemaMeta.apply
  )

  given RootJsonFormat[W3CSchemaPaginated] = jsonFormat4(
    W3CSchemaPaginated.apply
  )
}
