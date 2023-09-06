package io.iohk.atala.pollux.core.model

final case class IssuedCredentialRaw(
    signedCredential: String,
    format: String // TODO  mark a constant
)

/** @see
  *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0453-issue-credential-v2#offer-attachment-registry
  */
object IssuedCredentialRaw {
  final val formatPrismJWT = "prism/jwt" // TODO make it a standard and RENAME?
  final val formatPrismAnoncred = "prism/anoncred" // TODO make it a standard and RENAME?
}

//TODO make format it a sealed of a enum (with a fallback value like UnsupportedFormat)
