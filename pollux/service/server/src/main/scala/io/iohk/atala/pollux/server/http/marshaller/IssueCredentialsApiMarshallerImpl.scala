package io.iohk.atala.pollux.server.http.marshaller

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.pollux.openapi.api.IssueCredentialsApiMarshaller
import io.iohk.atala.pollux.openapi.model.{
  CreateCredentialsRequest,
  W3CIssuanceBatchAction,
  W3CCredentialInput,
  W3CCredentialsPaginated,
  W3CCredential,
  W3CIssuanceBatchPaginated,
  CreateCredentials201Response
}
import spray.json.RootJsonFormat
import zio.*

object IssueCredentialsApiMarshallerImpl extends JsonSupport {

  val layer: ULayer[IssueCredentialsApiMarshaller] = ZLayer.succeed {
    new IssueCredentialsApiMarshaller {

      implicit def fromEntityUnmarshallerCreateCredentialsRequest: FromEntityUnmarshaller[CreateCredentialsRequest] =
        summon[RootJsonFormat[CreateCredentialsRequest]]

      implicit def fromEntityUnmarshallerW3CIssuanceBatchActionList
          : FromEntityUnmarshaller[Seq[W3CIssuanceBatchAction]] = summon[RootJsonFormat[Seq[W3CIssuanceBatchAction]]]

      implicit def fromEntityUnmarshallerW3CCredentialInput: FromEntityUnmarshaller[W3CCredentialInput] =
        summon[RootJsonFormat[W3CCredentialInput]]

      implicit def toEntityMarshallerW3CCredentialsPaginated: ToEntityMarshaller[W3CCredentialsPaginated] =
        summon[RootJsonFormat[W3CCredentialsPaginated]]

      implicit def toEntityMarshallerW3CCredential: ToEntityMarshaller[W3CCredential] =
        summon[RootJsonFormat[W3CCredential]]

      implicit def toEntityMarshallerW3CIssuanceBatchPaginated: ToEntityMarshaller[W3CIssuanceBatchPaginated] =
        summon[RootJsonFormat[W3CIssuanceBatchPaginated]]

      implicit def toEntityMarshallerW3CIssuanceBatchActionarray: ToEntityMarshaller[Seq[W3CIssuanceBatchAction]]=
        summon[RootJsonFormat[Seq[W3CIssuanceBatchAction]]]

      implicit def toEntityMarshallerCreateCredentials201Response: ToEntityMarshaller[CreateCredentials201Response] =
        summon[RootJsonFormat[CreateCredentials201Response]]
    }
  }

}
