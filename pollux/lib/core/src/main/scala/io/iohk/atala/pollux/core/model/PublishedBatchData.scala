package io.iohk.atala.pollux.core.model

import io.iohk.atala.pollux.vc.jwt.W3CCredentialPayload
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.iris.proto.service.IrisOperationId

final case class PublishedBatchData(
    operationId: IrisOperationId,
    credentialsAnsProofs: Seq[(W3CCredentialPayload, MerkleInclusionProof)]
)
