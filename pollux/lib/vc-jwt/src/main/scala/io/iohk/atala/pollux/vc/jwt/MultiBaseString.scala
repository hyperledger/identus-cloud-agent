package io.iohk.atala.pollux.vc.jwt

import io.circe.*

case class MultiBaseString(header: MultiBaseString.Header, data: String) {
  def toMultiBaseString: String = s"${header.value}$data"
}

object MultiBaseString {
  enum Header(val value: Char) {
    case Base64Url extends Header('u')
    case Base58Btc extends Header('z')
  }

  object Header {
    def fromValue(value: Char): Option[Header] = value match {
      case 'u' => Some(Header.Base64Url)
      case 'z' => Some(Header.Base58Btc)
      case _   => None
    }
  }

  given multiBaseStringEncoder: Encoder[MultiBaseString] = (multiBaseString: MultiBaseString) =>
    Json.fromString(multiBaseString.toMultiBaseString)

  given multiBaseStringDecoder: Decoder[MultiBaseString] = (c: HCursor) =>
    Decoder.decodeString(c).flatMap { str =>
      val header = MultiBaseString.Header.fromValue(str.head)
      header match {
        case Some(value) => Right(MultiBaseString(value, str.tail))
        case None        => Left(DecodingFailure(s"no enum value matched for $str", List(CursorOp.Field(str))))
      }
    }
}
