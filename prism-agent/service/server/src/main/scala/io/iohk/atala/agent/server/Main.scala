package io.iohk.atala.agent.server

import zio.*

object Main extends ZIOAppDefault {
  override def run = Modules.app
}
