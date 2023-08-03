package io.iohk.atala.pollux.core.model

final case class IssuedCredentialRaw(
    signedCredential: String,
    format: String //
)
object IssuedCredentialRaw {
  final val formatPrismJWT = "prism/jwt" // TODO make it a standard and RENAME?
  final val formatPrismAnoncred = "prism/anoncred" // TODO make it a standard and RENAME?
}
