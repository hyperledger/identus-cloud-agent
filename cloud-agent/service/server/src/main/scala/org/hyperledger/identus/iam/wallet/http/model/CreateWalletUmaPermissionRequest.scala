package org.hyperledger.identus.iam.wallet.http.model

import org.hyperledger.identus.api.http.Annotation
import sttp.tapir.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.Schema.annotations.encodedExample
import sttp.tapir.Schema.annotations.validate
import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder

import java.util.UUID

final case class CreateWalletUmaPermissionRequest(
    @description(CreateWalletUmaPermissionRequest.annotations.subject.description)
    @encodedExample(CreateWalletUmaPermissionRequest.annotations.subject.example)
    subject: UUID
)

object CreateWalletUmaPermissionRequest {
  given encoder: JsonEncoder[CreateWalletUmaPermissionRequest] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[CreateWalletUmaPermissionRequest] = DeriveJsonDecoder.gen
  given schema: Schema[CreateWalletUmaPermissionRequest] = Schema.derived

  object annotations {
    object subject
        extends Annotation[UUID](
          description =
            "The subject ID that should be granted the permission to the wallet. This can be found in the `sub` claim of a JWT token.",
          example = UUID.fromString("00000000-0000-0000-0000-000000000000")
        )
  }
}
