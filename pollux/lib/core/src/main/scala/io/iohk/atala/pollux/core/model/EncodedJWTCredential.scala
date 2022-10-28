package io.iohk.atala.pollux.core.model

final case class EncodedJWTCredential(batchId: String, credentialId: String, content: String)
