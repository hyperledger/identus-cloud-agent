package io.iohk.atala.castor.core.model.did

enum ServiceType(val name: String) {
  case MediatorService extends ServiceType("MediatorService")
}

object ServiceType {

  private val lookup = ServiceType.values.map(i => i.name -> i).toMap

  def parseString(s: String): Option[ServiceType] = lookup.get(s)

}
