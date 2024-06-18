package org.hyperledger.identus.presentproof.controller.http

import org.hyperledger.identus.api.http.Annotation
import org.hyperledger.identus.presentproof.controller.http.PresentationStatusPage.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class PresentationStatusPage(
    @description(annotations.contents.description)
    @encodedExample( // This is a hammer - to be improved in the future
      JsonEncoder[Seq[PresentationStatus]].encodeJson(annotations.contents.example)
    )
    contents: Seq[PresentationStatus],
    @description(annotations.self.description)
    @encodedExample(annotations.self.example)
    self: String = "/present-proof/presentations",
    @description(annotations.kind.description)
    @encodedExample(annotations.kind.example)
    kind: String = "Collection",
    @description(annotations.pageOf.description)
    @encodedExample(annotations.pageOf.example)
    pageOf: String = "1",
    @description(annotations.next.description)
    @encodedExample(annotations.next.example)
    next: Option[String] = None,
    @description(annotations.previous.description)
    @encodedExample(annotations.previous.example)
    previous: Option[String] = None
)

object PresentationStatusPage {
  object annotations {
    object self
        extends Annotation[String](
          description = "The reference to the presentation collection itself.",
          example = "/present-proof/presentations"
        )
    object kind
        extends Annotation[String](
          description = "The type of object returned. In this case a `Collection`.",
          example = "Collection"
        )
    object pageOf
        extends Annotation[String](
          description = "Page number within the context of paginated response.",
          example = "1"
        )
    object next
        extends Annotation[String](
          description =
            "An optional string field containing the URL of the next page of results. If the API response does not contain any more pages, this field should be set to None.",
          example = "/present-proof/presentations?offset=20&limit=10"
        )
    object previous
        extends Annotation[String](
          description =
            "An optional string field containing the URL of the previous page of results. If the API response is the first page of results, this field should be set to None.",
          example = "/present-proof/presentations?offset=0&limit=10"
        )
    object contents
        extends Annotation[Seq[PresentationStatus]](
          description = "A sequence of Presentation objects.",
          example = Seq(
            PresentationStatus(
              presentationId = "938bfc23-f78d-4734-9bf3-6dccf300856f",
              thid = "04112f4d-e894-4bff-a706-85b3e7190a2c",
              role = "Verifier",
              status = "RequestSent",
              proofs = Seq.empty,
              data = Seq.empty,
              connectionId = Some("e0d81be9-47ca-4e0b-b8a7-325e8c3abc2f"),
              metaRetries = 5
            ),
            PresentationStatus(
              presentationId = "d22158b0-c650-48ea-be85-2920a845ef26",
              thid = "04112f4d-e894-4bff-a706-85b3e7190a2c",
              role = "Prover",
              status = "RequestReceived",
              proofs = Seq.empty,
              data = Seq.empty,
              metaRetries = 5
            ),
            PresentationStatus(
              presentationId = "fd3f5e54-fae9-4f72-9413-ec66aab83a57",
              thid = "6b42fd91-4c98-40ae-a371-a1fd1a39e05e",
              role = "Prover",
              status = "PresentationPending",
              proofs = Seq.empty,
              data = Seq.empty,
              metaRetries = 5
            ),
            PresentationStatus(
              presentationId = "e56dd3e0-79d0-45f4-ba6c-ff857211b07b",
              thid = "6b42fd91-4c98-40ae-a371-a1fd1a39e05e",
              role = "Verifier",
              status = "PresentationVerified",
              proofs = Seq.empty,
              data = Seq(
                "{\"claimsToDisclose\":{\"emailAddress\":{},\"givenName\":{}},\"presentation\":\"{\\\"protected\\\":\\\"eyJhbGciOiJFZERTQSJ9\\\",\\\"payload\\\":\\\"eyJfc2QiOlsiMGl4d0tIV0dzbzFvZThFR0hQd2tGYW9EZE1TRFQ3SmgyNkZGSm1ZbGRnRSIsIjQ4VlFXZS1tcjBibHMyOWpicHFKeDNxX2dYY0k5N3dHcEpsZnRoNXQwMGciLCI0Wk9xanFNZVNUVHRKQTNJRExsc3ZXN0dTNzRIemNxY3N2NVFoZk1valE4IiwiUjhGRE0ydXB1V09mNmVJMVA5ckNPdG12c3puVWFFYXpncVNuN0JfeTE0MCIsIlU5MmpfUHlpcHN2TERNQTlDaVRWbnl3bUFzYTM4S2lDWm5TeVhyUE5mNG8iLCJldFB1Mmc5ajdRd01rZ3g5VnpEX1RnNTNUV3UydVpadk1KeHRnNEJ1WGJBIiwidGV3RG1LWklNcS10bUNrMkpqZU0wajNYbU1aUUFLN01heENVNlF4dm9OMCJdLCJfc2RfYWxnIjoic2hhLTI1NiIsImlzcyI6ImRpZDpwcmlzbToxMmEzOWI1YWEwZTcxODI3ZmMxYzYwMjg1ZDVlZWJjMTk0Yjg2NzFhYTJmY2QxZDM2NDBkMGYwMTBlMzliZmVlIiwiaWF0IjoxNzE3NDEwMzgzLCJleHAiOjE3MjAwMDIzODN9\\\",\\\"signature\\\":\\\"953FfSRU_0Y2q0ERrFPzbXJ_hkF0YQe5efwESaZwtXDCn8aanD3MUstp3lzqGZkhvcWRdtCCpIxzhy0zgKwLBg\\\",\\\"disclosures\\\":[\\\"WyI0SHF6MDZCeG5fRlJMb2hWX2lWNXp3IiwgImdpdmVuTmFtZSIsICJBbGljZSJd\\\",\\\"WyJLUnNYYU01c3NXZTl4UEhqQnNjT213IiwgImVtYWlsQWRkcmVzcyIsICJhbGljZUB3b25kZXJsYW5kLmNvbSJd\\\"],\\\"kb_jwt\\\":null}\"}"
              ),
              connectionId = Some("e0d81be9-47ca-4e0b-b8a7-325e8c3abc2f"),
              metaRetries = 5
            ),
          )
        )
  }

  given encoder: JsonEncoder[PresentationStatusPage] =
    DeriveJsonEncoder.gen[PresentationStatusPage]

  given decoder: JsonDecoder[PresentationStatusPage] =
    DeriveJsonDecoder.gen[PresentationStatusPage]

  given schema: Schema[PresentationStatusPage] = Schema.derived
}
