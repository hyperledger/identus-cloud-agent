package io.iohk.atala.castor.controller

import zio.*

class DIDRegistrarControllerImpl() extends DIDRegistrarController {}

object DIDRegistrarControllerImpl {
  val layer: ULayer[DIDRegistrarController] = ZLayer.succeed(DIDRegistrarControllerImpl())
}
