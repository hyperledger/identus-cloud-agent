package io.iohk.atala.castor.worker

import zio.*

object Main extends ZIOAppDefault {
  override def run = Modules.app
}
