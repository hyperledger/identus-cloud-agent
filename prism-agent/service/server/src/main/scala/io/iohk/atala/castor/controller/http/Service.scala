package io.iohk.atala.castor.controller.http

import io.iohk.atala.api.http.codec.CirceJsonInterop
import io.iohk.atala.api.http.Annotation
import io.iohk.atala.castor.controller.http.Service.annotations
import io.iohk.atala.castor.core.model.did as castorDomain
import io.iohk.atala.castor.core.model.did.w3c
import io.iohk.atala.shared.utils.Traverse.*
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder, JsonError}
import io.lemonlabs.uri.Uri
import io.circe.Json
import io.iohk.atala.castor.core.model.ProtoModelHelper
import zio.json.internal.{RetractReader, Write}

@description("A service expressed in the DID document. https://www.w3.org/TR/did-core/#services")
final case class Service(
    @description(annotations.id.description)
    @encodedExample(annotations.id.example)
    id: String,
    @description(annotations.`type`.description)
    @encodedExample(annotations.`type`.example)
    `type`: ServiceType,
    @description(annotations.serviceEndpoint.description)
    @encodedExample(annotations.serviceEndpoint.example)
    serviceEndpoint: ServiceEndpoint
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
        extends Annotation[ServiceType](
          description =
            "Service type. Can contain multiple possible values as described in the [Create DID operation](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#create-did) under the construction section.",
          example = ServiceType.Single("LinkedDomains")
        )

    object serviceEndpoint
        extends Annotation[Json](
          description =
            "The service endpoint. Can contain multiple possible values as described in the [Create DID operation]",
          example = Json.fromString("https://example.com")
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
      serviceEndpoint = ServiceEndpoint.fromJson(service.serviceEndpoint)
    )

  extension (service: Service) {
    def toDomain: Either[String, castorDomain.Service] = {
      for {
        serviceEndpoint <- service.serviceEndpoint.toDomain
        serviceType <- service.`type`.toDomain
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

sealed trait ServiceType

object ServiceType {
  final case class Single(value: String) extends ServiceType
  final case class Multiple(values: Seq[String]) extends ServiceType

  given encoder: JsonEncoder[ServiceType] = JsonEncoder.string
    .orElseEither(JsonEncoder.array[String])
    .contramap[ServiceType] {
      case Single(value)    => Left(value)
      case Multiple(values) => Right(values.toArray)
    }
  given decoder: JsonDecoder[ServiceType] = JsonDecoder.string
    .orElseEither(JsonDecoder.array[String])
    .map[ServiceType] {
      case Left(value)   => Single(value)
      case Right(values) => Multiple(values.toSeq)
    }
  given schema: Schema[ServiceType] = Schema.derived[ServiceType]

  given Conversion[castorDomain.ServiceType, ServiceType] = {
    case t: castorDomain.ServiceType.Single   => Single(t.value.value)
    case t: castorDomain.ServiceType.Multiple => Multiple(t.values.map(_.value))
  }

  given Conversion[String | Seq[String], ServiceType] = {
    case s: String      => Single(s)
    case s: Seq[String] => Multiple(s)
  }

  extension (serviceType: ServiceType) {
    def toDomain: Either[String, castorDomain.ServiceType] = serviceType match {
      case Single(value) =>
        castorDomain.ServiceType.Name.fromString(value).map(castorDomain.ServiceType.Single.apply)
      case Multiple(values) =>
        values.toList match {
          case Nil => Left("serviceType cannot be empty")
          case head :: tail =>
            for {
              parsedHead <- castorDomain.ServiceType.Name.fromString(head)
              parsedTail <- tail.traverse(s => castorDomain.ServiceType.Name.fromString(s))
            } yield castorDomain.ServiceType.Multiple(parsedHead, parsedTail)
        }
    }
  }
}

opaque type ServiceEndpoint = Json

object ServiceEndpoint {
  given encoder: JsonEncoder[ServiceEndpoint] = CirceJsonInterop.encodeJson
  given decoder: JsonDecoder[ServiceEndpoint] = CirceJsonInterop.decodeJson
  given schema: Schema[ServiceEndpoint] = CirceJsonInterop.schemaJson

  def fromJson(json: Json): ServiceEndpoint = json

  extension (serviceEndpoint: ServiceEndpoint) {
    def toDomain: Either[String, castorDomain.ServiceEndpoint] = {
      val stringEncoded = serviceEndpoint.asString match {
        case Some(s) => s
        case None    => serviceEndpoint.noSpaces
      }
      ProtoModelHelper.parseServiceEndpoint(stringEncoded)
    }
  }
}
