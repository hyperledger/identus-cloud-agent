package io.iohk.atala.iris.apiserver

import zio.*

object Main extends ZIOAppDefault {
  override def run = Modules.app
}
