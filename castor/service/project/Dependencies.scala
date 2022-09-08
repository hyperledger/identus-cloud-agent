import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.2"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio

  lazy val baseDependencies: Seq[ModuleID] = Seq(zio)
}
