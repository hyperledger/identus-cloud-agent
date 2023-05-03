package io.iohk.atala.castor.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.castor.controller.http.Service.annotations
import io.iohk.atala.castor.core.model.did as castorDomain
import io.iohk.atala.castor.core.model.did.w3c
import io.iohk.atala.shared.utils.Traverse.*
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}
import io.lemonlabs.uri.Uri

@description("A service expressed in the DID document. https://www.w3.org/TR/did-core/#services")
final case class Service(
    @description(annotations.id.description)
    @encodedExample(annotations.id.example)
    id: String,
    @description(annotations.`type`.description)
    @encodedExample(annotations.`type`.example)
    `type`: String,
    @description(annotations.serviceEndpoint.description)
    serviceEndpoint: Seq[String]
)

object Service {

  object annotations {
    object id
        extends Annotation[String](
          description = """The id of the service.
              |Requires a URI fragment when use in create / update DID.
              |Returns the full ID (with DID prefix) when resolving DID""".stripMargin,
          example = "service-1"
        )

    object `type`
        extends Annotation[String](
          description =
            "Service type. Can contain multiple possible values as described in the [Create DID operation](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#create-did) under the construction section.",
          example = "LinkedDomains"
        )

    object serviceEndpoint
        extends Annotation[Seq[String]](
          description =
            "The service endpoint. Can contain multiple possible values as described in the [Create DID operation]",
          example = Seq("https://example.com")
        )
  }

  given encoder: JsonEncoder[Service] = DeriveJsonEncoder.gen[Service]
  given decoder: JsonDecoder[Service] = DeriveJsonDecoder.gen[Service]
  given schema: Schema[Service] = Schema
    .derived[Service]
    .modify(_.serviceEndpoint)(_.copy(isOptional = false))

  given Conversion[w3c.ServiceRepr, Service] = (service: w3c.ServiceRepr) =>
    Service(
      id = service.id,
      `type` = service.`type`,
      serviceEndpoint = service.serviceEndpoint
    )

  extension (service: Service) {
    def toDomain: Either[String, castorDomain.Service] = {
      for {
        serviceEndpoint <- service.serviceEndpoint
          .traverse(s => Uri.parseTry(s).toEither.left.map(_ => s"unable to parse serviceEndpoint $s as URI"))
        serviceType <- castorDomain.ServiceType
          .parseString(service.`type`)
          .toRight(s"unsupported serviceType ${service.`type`}")
      } yield castorDomain
        .Service(
          id = service.id,
          `type` = serviceType,
          serviceEndpoint = serviceEndpoint
        )
        .normalizeServiceEndpoint()
    }
  }
}
