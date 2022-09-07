import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.2"
  }

  lazy val zio = "dev.zio" %% "zio" % Versions.zio
}
