package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.did.w3c.W3CModelHelper.*
import io.iohk.atala.castor.core.model.did.w3c.makeW3CResolver
import io.iohk.atala.agent.openapi.api.DIDApiService
import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.agent.server.http.model.{HttpServiceError, OASDomainModelHelper, OASErrorModelHelper}
import io.iohk.atala.castor.core.model.error.DIDOperationError
import zio.*
import io.iohk.atala.agent.server.http.model.{OASModelPatches, CustomMediaTypes}
import io.iohk.atala.castor.core.model.did.w3c.DIDResolutionErrorRepr
import spray.json.{CompactPrinter, JsonWriter, RootJsonFormat}
import akka.http.scaladsl.marshalling.Marshaller

class DIDApiServiceImpl(service: DIDService)(using runtime: Runtime[Any])
    extends DIDApiService,
      AkkaZioSupport,
      OASDomainModelHelper,
      OASErrorModelHelper {

  override def getDid(didRef: String, accept: Option[String])(implicit
      toEntityMarshallerDIDResolutionResult: ToEntityMarshaller[DIDResolutionResult]
  ): Route = ???

  // FIXME
  def getDidFIXME(didRef: String, accept: Option[String])(implicit
      toEntityMarshallerDIDResolutionResult: ToEntityMarshaller[OASModelPatches.DIDResolutionResult]
  ): Route = {
    val result = for {
      result <- makeW3CResolver(service)(didRef).either
      resolutionResult = result.fold(_.toOAS, _.toOAS)
      resolutionError = result.swap.toOption
    } yield buildHttpBindingResponse(resolutionResult, resolutionError)

    onZioSuccess(result)(identity)
  }

  // Return response dynamically based on "Content-Type" negotiation
  // according to https://w3c-ccg.github.io/did-resolution/#bindings-https
  private def buildHttpBindingResponse(
      resolutionResult: OASModelPatches.DIDResolutionResult,
      resolutionError: Option[DIDResolutionErrorRepr]
  ): Route = {
    import io.iohk.atala.agent.server.http.marshaller.JsonSupport.{optionFormat, given}
    import DIDResolutionErrorRepr.*

    val jsonLdMarshaller: ToEntityMarshaller[OASModelPatches.DIDResolutionResult] = {
      val writer = optionFormat[OASModelPatches.DIDDocument]
      Marshaller.StringMarshaller
        .wrap(CustomMediaTypes.`application/did+ld+json`)(CompactPrinter)
        .compose(writer.write)
        .compose(_.didDocument)
    }
    val resolutionResultMarshaller: ToEntityMarshaller[OASModelPatches.DIDResolutionResult] = {
      val writer = summon[RootJsonFormat[OASModelPatches.DIDResolutionResult]]
      Marshaller.StringMarshaller
        .wrap(CustomMediaTypes.`application/ld+json;did-resolution`)(CompactPrinter)
        .compose(writer.write)
        .compose { result =>
          result.copy(didResolutionMetadata =
            result.didResolutionMetadata.copy(contentType = Some(CustomMediaTypes.`application/did+ld+json`.value))
          )
        }
    }

    given ToEntityMarshaller[OASModelPatches.DIDResolutionResult] =
      Marshaller.oneOf(resolutionResultMarshaller, jsonLdMarshaller)

    val isDeactivated = resolutionResult.didDocumentMetadata.deactivated.getOrElse(false)
    resolutionError match {
      case None if !isDeactivated           => complete(200 -> resolutionResult)
      case None                             => complete(410 -> resolutionResult)
      case Some(InvalidDID(_))              => complete(400 -> resolutionResult)
      case Some(InvalidDIDUrl(_))           => complete(400 -> resolutionResult)
      case Some(NotFound)                   => complete(404 -> resolutionResult)
      case Some(RepresentationNotSupported) => complete(406 -> resolutionResult)
      case Some(InternalError(_))           => complete(500 -> resolutionResult)
      case Some(_)                          => complete(500 -> resolutionResult)
    }
  }
}

object DIDApiServiceImpl {
  val layer: URLayer[DIDService, DIDApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[DIDService]
    } yield DIDApiServiceImpl(svc)(using rt)
  }
}
