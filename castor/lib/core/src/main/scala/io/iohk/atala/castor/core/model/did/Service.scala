package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.util.UriUtils
import io.lemonlabs.uri.Uri

final case class Service(
    id: String,
    `type`: ServiceType,
    serviceEndpoint: Seq[Uri]
) {
  def normalizeServiceEndpoint(): Service = {
    val normalizedUris = serviceEndpoint.flatMap { uri =>
      UriUtils.normalizeUri(uri.toString).map { normalizedUri =>
        Uri.parse(normalizedUri)
      }
    }
    this.copy(serviceEndpoint = normalizedUris)
  }
}
