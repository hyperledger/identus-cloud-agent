package org.hyperledger.identus.pollux.sdjwt

import zio.json.*

type Header = String
type Payload = String
type Signature = String
type Disclosure = String
type KBJWT = (String, String, String)

case class CredentialCompact(
    override val jwtHeader: Header,
    override val jwtPayload: Payload,
    override val jwtSignature: Signature,
    override val disclosures: Seq[Disclosure],
) extends ModelsExtensionMethods {
  override def kbJWT: Option[KBJWT] = None
}

object CredentialCompact {
  // given encoder: JsonEncoder[CredentialCompact] = DeriveJsonEncoder.gen[CredentialCompact]
  // given decoder: JsonDecoder[CredentialCompact] = DeriveJsonDecoder.gen[CredentialCompact]
  given decoder: JsonDecoder[CredentialCompact] = JsonDecoder.string.map(CredentialCompact.unsafeFromCompact(_))
  given encoder: JsonEncoder[CredentialCompact] = JsonEncoder.string.contramap[CredentialCompact](_.compact)

  // /**
  //   * // <Issuer-signed JWT>~<Disclosure 1>~<Disclosure 2>~...~<Disclosure N>~<optional KB-JWT>
  //   *
  //   * @param value
  //   * @return
  //   */
  def unsafeFromCompact(str: String): CredentialCompact = {
    import scala.util.matching.Regex
    // var str = "hhh.ppp.sss~a~b~xxxx"
    val patternCompact = new Regex("(^[\\w-]*)\\.([\\w-]*)\\.([\\w-]*)((?:~(?:[\\w-]*))*)~$")
    val patternDisclosure = new Regex("(~([\\w]+))")

    val patternCompact(h, p, s, disclosuresStr) = str: @unchecked
    CredentialCompact(
      jwtHeader = h,
      jwtPayload = p,
      jwtSignature = s,
      disclosures = patternDisclosure.findAllIn(disclosuresStr).toSeq.map(_.drop(1)),
    )
  }
}

case class PresentationCompact(
    override val jwtHeader: Header,
    override val jwtPayload: Payload,
    override val jwtSignature: Signature,
    override val disclosures: Seq[Disclosure],
    override val kbJWT: Option[KBJWT]
) extends ModelsExtensionMethods

object PresentationCompact {
  // given encoder: JsonEncoder[PresentationCompact] = DeriveJsonEncoder.gen[PresentationCompact]
  // given decoder: JsonDecoder[PresentationCompact] = DeriveJsonDecoder.gen[PresentationCompact]
  given decoder: JsonDecoder[PresentationCompact] = JsonDecoder.string.map(PresentationCompact.unsafeFromCompact(_))
  given encoder: JsonEncoder[PresentationCompact] = JsonEncoder.string.contramap[PresentationCompact](_.compact)

  // /**
  //   * // <Issuer-signed JWT>~<Disclosure 1>~<Disclosure 2>~...~<Disclosure N>~<optional KB-JWT>
  //   *
  //   * @param value
  //   * @return
  //   */
  def unsafeFromCompact(str: String): PresentationCompact = {
    import scala.util.matching.Regex
    // var str = "hhh.ppp.sss~a~b~xxxx"
    val patternCompact = new Regex(
      "^([\\w-]*)\\.([\\w-]*)\\.([\\w-]*)((?:~(?:[\\w-]*))*)~(?:([\\w-]*)\\.([\\w-]*)\\.([\\w-]*))?$"
    )
    val patternDisclosure = new Regex("(~([\\w]+))")

    val patternCompact(h, p, s, disclosuresStr, kb_h, kb_p, kb_s) = str: @unchecked

    val kbJWT: Option[KBJWT] = (
      Option(kb_h).filterNot(_.isBlank()),
      Option(kb_p).filterNot(_.isBlank()),
      Option(kb_s).filterNot(_.isBlank())
    ) match
      case (None, None, None)          => None
      case (Some(h), Some(p), Some(s)) => Some((h, p, s))
      case _                           => None // FIXME error

    PresentationCompact(
      jwtHeader = h,
      jwtPayload = p,
      jwtSignature = s,
      disclosures = patternDisclosure.findAllIn(disclosuresStr).toSeq.map(_.drop(1)),
      kbJWT = kbJWT,
    )
  }
}
