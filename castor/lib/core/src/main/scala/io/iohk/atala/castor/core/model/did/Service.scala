package io.iohk.atala.castor.core.model.did

import java.net.URI

final case class Service(
    id: String,
    `type`: ServiceType,
    serviceEndpoint: URI
)
