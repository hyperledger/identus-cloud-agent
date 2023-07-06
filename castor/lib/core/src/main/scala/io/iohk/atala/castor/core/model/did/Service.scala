package io.iohk.atala.castor.core.model.did

final case class Service(
    id: String,
    `type`: ServiceType,
    serviceEndpoint: ServiceEndpoint
) {

  def normalizeServiceEndpoint(): Service = copy(serviceEndpoint = serviceEndpoint.normalize())

}
