package io.iohk.atala.pollux.core.model

final case class IssuedCredentialRaw(
    signedCredential: String,
    format: String = "prism/jwt" // TODO  mark a constant
)
