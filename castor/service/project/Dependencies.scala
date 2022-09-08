import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.2"
    val http4s = "0.23.15"
  }

  lazy val zio = "dev.zio" %% "zio" % Versions.zio

  lazy val http4s = "org.http4s" %% "http4s-server" % Versions.http4s
  lazy val http4sCirce = "org.http4s" %% "http4s-circe" % Versions.http4s
  lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % Versions.http4s
  lazy val http4sClient = "org.http4s" %% "http4s-client" % Versions.http4s

  lazy val baseDependencies: Seq[ModuleID] = Seq(zio)
  lazy val httpDependencies: Seq[ModuleID] = Seq(http4s, http4sDsl, http4sCirce, http4sClient)
}
