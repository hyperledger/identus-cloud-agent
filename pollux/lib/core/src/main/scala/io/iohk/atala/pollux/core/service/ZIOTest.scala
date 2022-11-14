package io.iohk.atala.pollux.core.service

import zio._
import java.io.IOException

object ZIOTest extends ZIOAppDefault {

  def run = {
    val effect1: ZIO[Int, IOException, Int] = ZIO.succeed(5)
    val effect2: ZIO[String, IOException, String] = ZIO.succeed("Blabla")
    val effect3: ZIO[Double, IOException, Double] = ZIO.succeed(1234)

    val result2: ZIO[Int & String & Double, IOException, Double] = effect1.flatMap(r1 => effect2).flatMap(r2 => effect3)
    
    val layerInt: ULayer[Int] = ZLayer.succeed(5)
    val layerString: ULayer[String] = ZLayer.succeed("Blabla")
    val layerDouble: ULayer[Double] = ZLayer.succeed(5)

    val layer: ULayer[Int & String & Double] = layerInt  ++ layerDouble ++ layerString

    result2.provideLayer(layer)
  }

}
