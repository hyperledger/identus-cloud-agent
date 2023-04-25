package io.iohk.atala.api.http.codec

import io.iohk.atala.castor.controller.http.{DIDDocument, DIDResolutionResult}
import sttp.model.MediaType
import sttp.tapir.*
import sttp.tapir.DecodeResult.Error.{JsonDecodeException, JsonError}
import sttp.tapir.DecodeResult.{Error, Value}
import zio.json.{EncoderOps, JsonCodec, JsonDecoder, JsonEncoder}

object DIDCodec {

  final case class DIDJsonLD() extends CodecFormat {
    override val mediaType: MediaType = CustomMediaTypes.`application/did+ld+json`
  }

  final case class DIDResolutionJsonLD() extends CodecFormat {
    override val mediaType: MediaType = CustomMediaTypes.`application/ld+json;did-resolution`
  }

  def didJsonLD: Codec[String, DIDResolutionResult, DIDJsonLD] = {
    val errorMsg = "Decoding application/did+ld+json resource is not supported"
    val didDocumentCodec = sttp.tapir.json.zio.zioCodec[Option[DIDDocument]]
    Codec
      .json[DIDResolutionResult](_ => DecodeResult.Error(errorMsg, Exception(errorMsg)))(resolutionResult =>
        didDocumentCodec.encode(resolutionResult.didDocument)
      )
      .schema(didDocumentCodec.schema.map[DIDResolutionResult](_ => None)(_.didDocument))
      .format(DIDJsonLD())
  }

  def didResolutionJsonLD: Codec[String, DIDResolutionResult, DIDResolutionJsonLD] = {
    val codecFormat = DIDResolutionJsonLD()
    sttp.tapir.json.zio
      .zioCodec[DIDResolutionResult]
      .format(codecFormat)
      .map[DIDResolutionResult](i => i)(resolutionResult =>
        // inject the contentType to the response body based on the content negotiation result
        // https://www.w3.org/TR/did-core/#did-resolution-metadata
        resolutionResult
          .copy(didResolutionMetadata =
            resolutionResult.didResolutionMetadata
              .copy(contentType = Some(codecFormat.mediaType.toString))
          )
      )
  }

}
