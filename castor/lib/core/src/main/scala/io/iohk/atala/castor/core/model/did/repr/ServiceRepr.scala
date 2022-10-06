package io.iohk.atala.castor.core.model.did.repr

import java.net.URI
import io.iohk.atala.castor.core.model.did.DIDUrl

final case class ServiceRepr(
    id: String,
    `type`: String,
    serviceEndpoint: String
)
