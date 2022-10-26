package io.iohk.atala.pollux.vc.jwt

import io.circe
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import net.reactivecore.cjs.resolver.Downloader
import net.reactivecore.cjs.{DocumentValidator, Loader, Result}
import pdi.jwt.algorithms.JwtECDSAAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.security.*
import java.security.spec.*
import java.sql.Timestamp
import java.time.{Instant, ZonedDateTime}

opaque type JWT = String

object JWT {
  def apply(value: String): JWT = value

  extension (jwt: JWT) {
    def value: String = jwt
  }
}

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
  def encode(claim: Json): JWT
}

class ES256Signer(privateKey: PrivateKey) extends Signer {
  val algorithm: JwtECDSAAlgorithm = JwtAlgorithm.ES256

  override def encode(claim: Json): JWT = {
    return JWT(JwtCirce.encode(claim, privateKey, algorithm))
  }
}
