package io.iohk.atala.pollux.vc.jwt

import java.security.PrivateKey
import java.security.spec.ECGenParameterSpec
import java.sql.Timestamp
import java.time.{Instant, ZonedDateTime}
import io.circe.*
import pdi.jwt.JwtClaim
import pdi.jwt.{JwtAlgorithm, JwtCirce}

import java.security.*
import java.security.spec.*
import java.time.Instant
import net.reactivecore.cjs.Loader
import net.reactivecore.cjs.{DocumentValidator, Loader, Result}
import net.reactivecore.cjs.resolver.Downloader
import cats.implicits.*
import io.circe.Json
import pdi.jwt.algorithms.JwtECDSAAlgorithm

case class EncodedJWT(jwt: String)
case class JWTHeader(typ: String = "JWT", alg: Option[String])
case class JWTPayload(
    iss: Option[String],
    sub: Option[String],
    aud: Vector[String],
    iat: Option[Instant],
    nbf: Option[Instant],
    exp: Option[Instant],
    rexp: Option[Instant]
)
trait JWTVerified(
    verified: Boolean,
    payload: JWTPayload,
    didResolutionResult: DIDResolutionResult,
    issuer: String,
    signer: VerificationMethod,
    jwt: String,
    policies: Option[JWTVerifyPolicies]
)

case class JWTVerifyPolicies(
    now: Option[Boolean],
    nbf: Option[Boolean],
    exp: Option[Boolean],
    aud: Vector[Boolean]
)

trait Signer {
  def encode(claim: Json): String
}

class ES256Signer(privateKey: PrivateKey) extends Signer {
  val algorithm: JwtECDSAAlgorithm = JwtAlgorithm.ES256
  override def encode(claim: Json): String = {
    return JwtCirce.encode(claim, privateKey, algorithm)
  }
}
