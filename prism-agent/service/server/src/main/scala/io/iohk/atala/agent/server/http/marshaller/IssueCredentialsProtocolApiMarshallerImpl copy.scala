package io.iohk.atala.agent.server.http.marshaller

import zio.*
import spray.json.RootJsonFormat
import io.iohk.atala.pollux.core.service.CredentialService
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.agent.openapi.api.PresentProofApiMarshaller

object PresentProofApiMarshallerImpl extends JsonSupport {
  val layer: ULayer[PresentProofApiMarshaller] = ZLayer.succeed {
    new PresentProofApiMarshaller {

      implicit def fromEntityUnmarshallerRequestPresentationOutput: FromEntityUnmarshaller[RequestPresentationOutput] =
        summon[RootJsonFormat[RequestPresentationOutput]]

    }
  }
}
