import sbt._

object SharedDependencies {
  object Versions {
    val typesafeConfig = "1.4.2"
    val protobuf = "3.1.9"
    val testContainersScalaPostgresql = "0.40.11"
  }

  private lazy val typesafeConfig = "com.typesafe" % "config" % Versions.typesafeConfig
  private lazy val scalaPbGrpc =
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
  private lazy val testcontainers =
    "com.dimafeng" %% "testcontainers-scala-postgresql" % Versions.testContainersScalaPostgresql

  lazy val dependencies: Seq[ModuleID] = Seq(typesafeConfig, scalaPbGrpc, testcontainers)
}
