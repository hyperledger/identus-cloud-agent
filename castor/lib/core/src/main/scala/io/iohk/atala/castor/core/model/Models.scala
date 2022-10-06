package io.iohk.atala.castor.core.model

import io.iohk.atala.castor.core.model.did.{DID, DIDDocument}

// TODO: replace with actual implementation
final case class IrisNotification(foo: String)

final case class PublishedDIDOperationSubmissionResult(
    id: DID,
    document: DIDDocument,
    deactivated: Boolean,
    dltOperationId: String // TODO: refine this
)
