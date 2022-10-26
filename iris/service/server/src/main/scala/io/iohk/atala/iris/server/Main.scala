package io.iohk.atala.iris.server

import zio.*
import zio.stream.*

object Main extends ZIOAppDefault {
  override def run = Modules.app
}
