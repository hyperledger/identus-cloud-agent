package io.iohk.atala.pollux.vc.jwt

import io.circe
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import pdi.jwt.{Jwt, JwtCirce}
import zio.prelude.*

import java.security.{KeyPairGenerator, PublicKey}
import java.time.{Instant, ZonedDateTime}
import scala.util.{Failure, Success, Try}

sealed trait VerifiablePresentationPayload

case class W3cVerifiablePresentationPayload(payload: W3cPresentationPayload, proof: Proof)
    extends Verifiable(proof),
      VerifiablePresentationPayload

case class JwtVerifiablePresentationPayload(jwt: JWT) extends VerifiablePresentationPayload

sealed trait PresentationPayload(
    `@context`: IndexedSeq[String],
    `type`: IndexedSeq[String],
    verifiableCredential: IndexedSeq[VerifiableCredentialPayload],
    iss: String,
    maybeNbf: Option[Instant],
    aud: IndexedSeq[String],
    maybeExp: Option[Instant],
    maybeJti: Option[String],
    maybeNonce: Option[String]
) {
  def toJwtPresentationPayload: JwtPresentationPayload =
    JwtPresentationPayload(
      iss = iss,
      vp = JwtVp(
        `@context` = `@context`,
        `type` = `type`,
        verifiableCredential = verifiableCredential
      ),
      maybeNbf = maybeNbf,
      aud = aud,
      maybeExp = maybeExp,
      maybeJti = maybeJti,
      maybeNonce = maybeNonce
    )

  def toW3CPresentationPayload: W3cPresentationPayload =
    W3cPresentationPayload(
      `@context` = `@context`.distinct,
      maybeId = maybeJti,
      `type` = `type`.distinct,
      verifiableCredential = verifiableCredential,
      holder = iss,
      verifier = aud,
      maybeIssuanceDate = maybeNbf,
      maybeExpirationDate = maybeExp,
      maybeNonce = maybeNonce
    )
}

case class W3cPresentationPayload(
    `@context`: IndexedSeq[String],
    maybeId: Option[String],
    `type`: IndexedSeq[String],
    verifiableCredential: IndexedSeq[VerifiableCredentialPayload],
    holder: String,
    verifier: IndexedSeq[String],
    maybeIssuanceDate: Option[Instant],
    maybeExpirationDate: Option[Instant],

    /** Not part of W3C Presentation but included to preserve in case of conversion from JWT. */
    maybeNonce: Option[String] = Option.empty
) extends PresentationPayload(
      `@context` = `@context`.distinct,
      `type` = `type`.distinct,
      maybeJti = maybeId,
      verifiableCredential = verifiableCredential,
      aud = verifier,
      iss = holder,
      maybeNbf = maybeIssuanceDate,
      maybeExp = maybeExpirationDate,
      maybeNonce = maybeNonce
    )

case class JwtVp(
    `@context`: IndexedSeq[String],
    `type`: IndexedSeq[String],
    verifiableCredential: IndexedSeq[VerifiableCredentialPayload]
)

case class JwtPresentationPayload(
    iss: String,
    vp: JwtVp,
    maybeNbf: Option[Instant],
    aud: IndexedSeq[String],
    maybeExp: Option[Instant],
    maybeJti: Option[String],
    maybeNonce: Option[String]
) extends PresentationPayload(
      iss = iss,
      `@context` = vp.`@context`,
      `type` = vp.`type`,
      verifiableCredential = vp.verifiableCredential,
      maybeNbf = maybeNbf,
      aud = aud,
      maybeExp = maybeExp,
      maybeJti = maybeJti,
      maybeNonce = maybeNonce
    )

object PresentationPayload {

  object Implicits {

    import CredentialPayload.Implicits.*
    import Proof.Implicits.*

    implicit val w3cPresentationPayloadEncoder: Encoder[W3cPresentationPayload] =
      (w3cPresentationPayload: W3cPresentationPayload) =>
        Json
          .obj(
            ("@context", w3cPresentationPayload.`@context`.asJson),
            ("id", w3cPresentationPayload.maybeId.asJson),
            ("type", w3cPresentationPayload.`type`.asJson),
            ("verifiableCredential", w3cPresentationPayload.verifiableCredential.asJson),
            ("holder", w3cPresentationPayload.holder.asJson),
            ("verifier", w3cPresentationPayload.verifier.asJson),
            ("issuanceDate", w3cPresentationPayload.maybeIssuanceDate.asJson),
            ("expirationDate", w3cPresentationPayload.maybeExpirationDate.asJson)
          )
          .deepDropNullValues
          .dropEmptyValues

    implicit val jwtVpEncoder: Encoder[JwtVp] =
      (jwtVp: JwtVp) =>
        Json
          .obj(
            ("@context", jwtVp.`@context`.asJson),
            ("type", jwtVp.`type`.asJson),
            ("verifiableCredential", jwtVp.verifiableCredential.asJson)
          )
          .deepDropNullValues
          .dropEmptyValues

    implicit val jwtPresentationPayloadEncoder: Encoder[JwtPresentationPayload] =
      (jwtPresentationPayload: JwtPresentationPayload) =>
        Json
          .obj(
            ("iss", jwtPresentationPayload.iss.asJson),
            ("vp", jwtPresentationPayload.vp.asJson),
            ("nbf", jwtPresentationPayload.maybeNbf.asJson),
            ("aud", jwtPresentationPayload.aud.asJson),
            ("exp", jwtPresentationPayload.maybeExp.asJson),
            ("jti", jwtPresentationPayload.maybeJti.asJson),
            ("nonce", jwtPresentationPayload.maybeNonce.asJson)
          )
          .deepDropNullValues
          .dropEmptyValues

    implicit val w3cPresentationPayloadDecoder: Decoder[W3cPresentationPayload] =
      (c: HCursor) =>
        for {
          `@context` <- c
            .downField("@context")
            .as[IndexedSeq[String]]
            .orElse(c.downField("@context").as[String].map(IndexedSeq(_)))
          maybeId <- c.downField("id").as[Option[String]]
          `type` <- c
            .downField("type")
            .as[IndexedSeq[String]]
            .orElse(c.downField("type").as[String].map(IndexedSeq(_)))
          holder <- c.downField("issuer").as[String]
          verifiableCredential <- c
            .downField("verifiableCredential")
            .as[Option[VerifiableCredentialPayload]]
            .map(_.iterator.toIndexedSeq)
            .orElse(
              c.downField("verifiableCredential")
                .as[Option[IndexedSeq[VerifiableCredentialPayload]]]
                .map(_.iterator.toIndexedSeq.flatten)
            )
          verifier <- c
            .downField("verifier")
            .as[Option[String]]
            .map(_.iterator.toIndexedSeq)
            .orElse(c.downField("verifier").as[Option[IndexedSeq[String]]].map(_.iterator.toIndexedSeq.flatten))
          maybeIssuanceDate <- c.downField("issuanceDate").as[Option[Instant]]
          maybeExpirationDate <- c.downField("expirationDate").as[Option[Instant]]
        } yield {
          W3cPresentationPayload(
            `@context` = `@context`.distinct,
            maybeId = maybeId,
            `type` = `type`.distinct,
            verifiableCredential = verifiableCredential.distinct,
            holder = holder,
            verifier = verifier.distinct,
            maybeIssuanceDate = maybeIssuanceDate,
            maybeExpirationDate = maybeExpirationDate,
            maybeNonce = Option.empty
          )
        }

    implicit val jwtVpDecoder: Decoder[JwtVp] =
      (c: HCursor) =>
        for {
          `@context` <- c
            .downField("@context")
            .as[IndexedSeq[String]]
            .orElse(c.downField("@context").as[String].map(IndexedSeq(_)))
          `type` <- c
            .downField("type")
            .as[IndexedSeq[String]]
            .orElse(c.downField("type").as[String].map(IndexedSeq(_)))
          maybeVerifiableCredential <- c
            .downField("verifiableCredential")
            .as[Option[IndexedSeq[VerifiableCredentialPayload]]]
        } yield {
          JwtVp(
            `@context` = `@context`.distinct,
            `type` = `type`.distinct,
            verifiableCredential = maybeVerifiableCredential.toIndexedSeq.flatten
          )
        }

    implicit val JwtPresentationPayloadDecoder: Decoder[JwtPresentationPayload] =
      (c: HCursor) =>
        for {
          iss <- c.downField("iss").as[String]
          vp <- c.downField("vp").as[JwtVp]
          maybeNbf <- c.downField("nbf").as[Option[Instant]]
          aud <- c
            .downField("aud")
            .as[Option[String]]
            .map(_.iterator.toIndexedSeq)
            .orElse(c.downField("aud").as[Option[IndexedSeq[String]]].map(_.iterator.toIndexedSeq.flatten))
          maybeExp <- c.downField("exp").as[Option[Instant]]
          maybeJti <- c.downField("jti").as[Option[String]]
          maybeNonce <- c.downField("nonce").as[Option[String]]
        } yield {
          JwtPresentationPayload(
            iss = iss,
            vp = vp,
            maybeNbf = maybeNbf,
            aud = aud.distinct,
            maybeExp = maybeExp,
            maybeJti = maybeJti,
            maybeNonce = maybeNonce
          )
        }

    implicit val w3cVerifiablePresentationPayloadDecoder: Decoder[W3cVerifiablePresentationPayload] =
      (c: HCursor) =>
        for {
          payload <- c.as[W3cPresentationPayload]
          proof <- c.downField("proof").as[Proof]
        } yield {
          W3cVerifiablePresentationPayload(
            payload = payload,
            proof = proof
          )
        }

    implicit val jwtVerifiablePresentationPayloadDecoder: Decoder[JwtVerifiablePresentationPayload] =
      (c: HCursor) =>
        for {
          jwt <- c.as[String]
        } yield {
          JwtVerifiablePresentationPayload(
            jwt = JWT(jwt)
          )
        }

    implicit val verifiablePresentationPayloadDecoder: Decoder[VerifiablePresentationPayload] =
      jwtVerifiablePresentationPayloadDecoder.or(
        w3cVerifiablePresentationPayloadDecoder.asInstanceOf[Decoder[VerifiablePresentationPayload]]
      )
  }
}

object JwtPresentation {

  import PresentationPayload.Implicits.*

  def encodeJwt(payload: JwtPresentationPayload, issuer: Issuer): JWT = issuer.signer.encode(payload.asJson)

  def toEncodedJwt(payload: W3cPresentationPayload, issuer: Issuer): JWT =
    encodeJwt(payload.toJwtPresentationPayload, issuer)

  def decodeJwt(jwt: JWT, publicKey: PublicKey): Try[JwtPresentationPayload] = {
    JwtCirce.decodeRaw(jwt.value, publicKey).flatMap(decode[JwtPresentationPayload](_).toTry)
  }

  def validateEncodedJwt(jwt: JWT, publicKey: PublicKey): Boolean =
    JwtCirce.isValid(jwt.value, publicKey)
}
