package io.iohk.atala.presentproof.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.mercury.model.Base64
import io.iohk.atala.pollux.core.model.PresentationRecord
import io.iohk.atala.presentproof.controller.http.PresentationStatus.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class PresentationStatus(
    @description(annotations.presentationId.description)
    @encodedExample(annotations.presentationId.example)
    presentationId: String,
    @description(annotations.status.description)
    @encodedExample(annotations.status.example)
    status: String,
    @description(annotations.proofs.description)
    @encodedExample(annotations.proofs.example)
    proofs: Seq[ProofRequestAux],
    @description(annotations.data.description)
    @encodedExample(annotations.data.example)
    data: Seq[String],
    @description(annotations.connectionId.description)
    @encodedExample(annotations.connectionId.example)
    connectionId: Option[String] = None
)

object PresentationStatus {
  def fromDomain(domain: PresentationRecord): PresentationStatus = {
    val data = domain.presentationData match
      case Some(p) =>
        p.attachments.head.data match {
          case Base64(data) =>
            val base64Decoded = new String(java.util.Base64.getDecoder.decode(data))
            println(s"Base64decode:\n\n ${base64Decoded} \n\n")
            Seq(base64Decoded)
          case any => ???
        }
      case None => Seq.empty
    PresentationStatus(
      domain.id.value,
      status = domain.protocolState.toString,
      proofs = Seq.empty,
      data = data,
      connectionId = domain.connectionId
    )
  }

  object annotations {
    object presentationId
        extends Annotation[String](
          description = "",
          example = ""
        )
    object status
        extends Annotation[String](
          description = "",
          example = ""
        )
    object proofs
        extends Annotation[String](
          description = "",
          example = ""
        )
    object data
        extends Annotation[String](
          description = "",
          example = ""
        )
    object connectionId
        extends Annotation[String](
          description = "",
          example = ""
        )
  }

  given encoder: JsonEncoder[PresentationStatus] =
    DeriveJsonEncoder.gen[PresentationStatus]

  given decoder: JsonDecoder[PresentationStatus] =
    DeriveJsonDecoder.gen[PresentationStatus]

  given schema: Schema[PresentationStatus] = Schema.derived
}
