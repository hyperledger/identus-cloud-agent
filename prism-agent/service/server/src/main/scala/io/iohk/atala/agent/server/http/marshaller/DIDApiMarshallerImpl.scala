package io.iohk.atala.agent.server.http.marshaller

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.agent.openapi.api.DIDApiMarshaller
import io.iohk.atala.agent.openapi.model.{DIDOperationResponse, ErrorResponse}
import spray.json.RootJsonFormat
import zio.*
import io.iohk.atala.agent.server.http.model.OASModelPatches

object DIDApiMarshallerImpl extends JsonSupport {

  val layer: ULayer[DIDApiMarshaller] = ZLayer.succeed {
    new DIDApiMarshaller {
      // implicit def toEntityMarshallerDIDResolutionResult: ToEntityMarshaller[OASModelPatches.DIDResolutionResult] =
      //   summon[RootJsonFormat[OASModelPatches.DIDResolutionResult]]

      implicit def toEntityMarshallerDIDResolutionResult: akka.http.scaladsl.marshalling.ToEntityMarshaller[
        io.iohk.atala.agent.openapi.model.DIDResolutionResult
      ] = ??? // FIXME
    }
  }

}
