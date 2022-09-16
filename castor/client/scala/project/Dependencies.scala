import sbt._

object Dependencies {

  object Versions {
    val sttp = "3.7.6"
    val circe = "0.14.3"
  }

  private lazy val scalaPbProto = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  private lazy val scalaPbGrpc = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion

  private lazy val circe = "io.circe" %% "circe-core" % Versions.circe
  private lazy val circeGeneric = "io.circe" %% "circe-generic" % Versions.circe
  private lazy val circeParser = "io.circe" %% "circe-parser" % Versions.circe

  private lazy val sttpCore = "com.softwaremill.sttp.client3" %% "core" % Versions.sttp
  private lazy val sttpCirce = "com.softwaremill.sttp.client3" %% "circe" % Versions.sttp

  private lazy val circeDependencies: Seq[ModuleID] = Seq(circe, circeGeneric, circeParser)
  private lazy val httpDependencies: Seq[ModuleID] = Seq(sttpCore, sttpCirce)
  private lazy val grpcDependencies: Seq[ModuleID] = Seq(scalaPbProto, scalaPbGrpc)

  lazy val clientDependencies: Seq[ModuleID] = circeDependencies ++ httpDependencies ++ grpcDependencies

}
