package io.iohk.atala.castor.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.castor.core.model.did.w3c
import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}
import sttp.tapir.Schema.annotations.{description, encodedExample}
import io.iohk.atala.castor.controller.http.DIDDocumentMetadata.annotations

@description("[DID document metadata](https://www.w3.org/TR/did-core/#did-document-metadata)")
final case class DIDDocumentMetadata(
    @description(annotations.deactivated.description)
    @encodedExample(annotations.deactivated.example)
    deactivated: Option[Boolean] = None,
    @description(annotations.canonicalId.description)
    @encodedExample(annotations.canonicalId.example)
    canonicalId: Option[String] = None
)

object DIDDocumentMetadata {

  object annotations {
    object deactivated
        extends Annotation[Boolean](
          description =
            "If a DID has been deactivated, DID document metadata MUST include this property with the boolean value true. If a DID has not been deactivated, this property is OPTIONAL, but if included, MUST have the boolean value false.",
          example = false
        )

    object canonicalId
        extends Annotation[String](
          description = """
            |A DID in canonical form.
            |If a DID is in long form and has been published, DID document metadata MUST contain a `canonicalId`` property with the short form DID as its value.
            |If a DID in short form or has not been published, DID document metadata MUST NOT contain a `canonicalId` property.
            |""".stripMargin,
          example = "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff"
        )
  }

  given encoder: JsonEncoder[DIDDocumentMetadata] = DeriveJsonEncoder.gen[DIDDocumentMetadata]
  given decoder: JsonDecoder[DIDDocumentMetadata] = DeriveJsonDecoder.gen[DIDDocumentMetadata]
  given schema: Schema[DIDDocumentMetadata] = Schema.derived

  given Conversion[w3c.DIDDocumentMetadataRepr, DIDDocumentMetadata] =
    (didDocumentMetadata: w3c.DIDDocumentMetadataRepr) =>
      DIDDocumentMetadata(
        deactivated = Some(didDocumentMetadata.deactivated),
        canonicalId = Some(didDocumentMetadata.canonicalId)
      )
}
