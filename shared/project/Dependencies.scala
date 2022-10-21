import sbt._

object Dependencies {
  object Versions {
    val typesafeConfig = "1.4.2"
    val protobuf = "3.1.9"
  }

  private lazy val typesafeConfig = "com.typesafe" % "config" % Versions.typesafeConfig
  private lazy val scalaPbGrpc = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion

  lazy val dependencies: Seq[ModuleID] = Seq(typesafeConfig, scalaPbGrpc)
}
