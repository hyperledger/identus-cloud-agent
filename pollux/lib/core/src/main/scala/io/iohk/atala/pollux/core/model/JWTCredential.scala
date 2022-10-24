package io.iohk.atala.pollux.core.model
import io.iohk.atala.pollux.vc.jwt.JwtCredentialPayload

final case class JWTCredential(batchId: String, credentialId: String, content: JwtCredentialPayload)
