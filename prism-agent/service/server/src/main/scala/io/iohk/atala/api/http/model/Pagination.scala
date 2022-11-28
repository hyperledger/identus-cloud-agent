package io.iohk.atala.api.http.model

import sttp.tapir.Codec.PlainCodec
import sttp.tapir.generic.auto.*
import sttp.tapir.{Codec, DecodeResult, Schema}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

import java.time.ZonedDateTime
import java.util.{Base64, UUID}
import scala.util.Try

case class Pagination(
    offset: Option[Int] = Some(0),
    limit: Option[Int] = Some(10)
)
