import java.io.IOException

import zio._

val effect1: ZIO[Int, IOException, Int] = ZIO.succeed(5)
val effect2: ZIO[String, IOException, String] = ZIO.succeed("Blabla")
val effect3: ZIO[Double, IOException, Double] = ZIO.succeed(1234)

// val result: ZIO[Any, Nothing, Unit] = for {
//     _ <- effect1
//     _ <- effect2
// } yield ()


val result2: ZIO[Int & String & Double, IOException, Double] = effect1.flatMap(r1 => effect2).flatMap(r2 => effect3)