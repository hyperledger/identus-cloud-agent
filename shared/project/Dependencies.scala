import sbt._

object Dependencies {
  object Versions {
    val typesafeConfig = "1.4.2"
  }

  private lazy val typesafeConfig = "com.typesafe" % "config" % Versions.typesafeConfig

  lazy val dependencies: Seq[ModuleID] = Seq(typesafeConfig)
}
