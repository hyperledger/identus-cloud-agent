package io.iohk.atala.castor.httpserver

import zio.*

object Main extends ZIOAppDefault {

  override def run = Layers.app

}
