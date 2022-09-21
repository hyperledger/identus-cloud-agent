package io.iohk.atala.castor.server

import zio.*

object Main extends ZIOAppDefault {
  override def run = Modules.app
}
