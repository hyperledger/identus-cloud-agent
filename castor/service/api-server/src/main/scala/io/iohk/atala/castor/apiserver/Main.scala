package io.iohk.atala.castor.apiserver

import zio.*

object Main extends ZIOAppDefault {
  override def run = Modules.app
}
