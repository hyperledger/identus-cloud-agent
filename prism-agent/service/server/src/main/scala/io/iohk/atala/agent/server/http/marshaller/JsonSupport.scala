package io.iohk.atala.agent.server.http.marshaller

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.iohk.atala.agent.openapi.model.*
import spray.json.{
  DefaultJsonProtocol,
  DeserializationException,
  JsString,
  JsValue,
  JsonFormat,
  RootJsonFormat,
  jsonReader,
  jsonWriter
}

import java.time.OffsetDateTime
import java.util.UUID

object JsonSupport extends JsonSupport

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  given RootJsonFormat[ErrorResponse] = jsonFormat5(ErrorResponse.apply)

  // Issue Credential Protocol
  implicit object UUIDFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)
    def read(value: JsValue) = {
      value match {
        case JsString(uuid) => UUID.fromString(uuid)
        case _              => throw new DeserializationException("Expected hexadecimal UUID string")
      }
    }
  }
  implicit object OffsetDateTimeFormat extends JsonFormat[OffsetDateTime] {
    def write(dt: OffsetDateTime) = JsString(dt.toString)
    def read(value: JsValue) = {
      value match {
        case JsString(dt) => OffsetDateTime.parse(dt)
        case _            => throw new DeserializationException("Expected hexadecimal OffsetDateTime string")
      }
    }
  }

  // Presentation
  given RootJsonFormat[Options] = jsonFormat2(Options.apply)
  given RootJsonFormat[ProofRequestAux] = jsonFormat2(ProofRequestAux.apply)
  given RootJsonFormat[RequestPresentationInput] = jsonFormat3(RequestPresentationInput.apply)
  given RootJsonFormat[RequestPresentationOutput] = jsonFormat1(RequestPresentationOutput.apply)
  given RootJsonFormat[PresentationStatus] = jsonFormat5(PresentationStatus.apply)
  given RootJsonFormat[RequestPresentationAction] = jsonFormat2(RequestPresentationAction.apply)
  given RootJsonFormat[PresentationStatusPage] = jsonFormat6(PresentationStatusPage.apply)
}
