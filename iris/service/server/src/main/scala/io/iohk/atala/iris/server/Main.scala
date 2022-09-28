package io.iohk.atala.iris.server

import zio.*

object Main extends ZIOAppDefault {
  override def run = Modules.app
}
