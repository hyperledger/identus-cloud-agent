import sbt._

object Dependencies {

  object Versions {
    val sttp = "3.7.6"
  }

  private lazy val scalaPbProto = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  private lazy val scalaPbGrpc = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion

  private lazy val sttpCore = "com.softwaremill.sttp.client3" %% "core" % Versions.sttp
  private lazy val sttpCirce = "com.softwaremill.sttp.client3" %% "circe" % Versions.sttp

  private lazy val httpDependencies: Seq[ModuleID] = Seq(sttpCore, sttpCirce)
  private lazy val grpcDependencies: Seq[ModuleID] = Seq(scalaPbProto, scalaPbGrpc)

  lazy val clientDependencies: Seq[ModuleID] = httpDependencies ++ grpcDependencies

}
