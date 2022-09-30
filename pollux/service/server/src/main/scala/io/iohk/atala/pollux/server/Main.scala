package io.iohk.atala.pollux.server

import zio.*

object Main extends ZIOAppDefault {
  override def run = Modules.app
}
