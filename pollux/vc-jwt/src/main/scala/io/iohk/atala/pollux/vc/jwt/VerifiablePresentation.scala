package io.iohk.atala.pollux.vc.jwt

trait W3CPresentation(
    `@context`: Vector[String],
    `type`: Vector[String],
    verifier: Vector[String],
    verifiableCredential: Vector[W3CCredential]
)

trait VerifiablePresentation
trait W3CVerifiablePresentation extends W3CPresentation, Verifiable
trait JWTVerifiablePresentation(jwt: EncodedJWT) extends VerifiablePresentation
trait VerifiedPresentation extends JWTVerified, W3CVerifiablePresentation
