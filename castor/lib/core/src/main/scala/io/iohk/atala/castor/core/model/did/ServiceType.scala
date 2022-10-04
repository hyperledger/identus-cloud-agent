package io.iohk.atala.castor.core.model.did

sealed trait ServiceType

object ServiceType {
  case object MediatorService extends ServiceType
}
