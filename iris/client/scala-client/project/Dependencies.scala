import sbt._

object Dependencies {

  private lazy val scalaPbProto = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  private lazy val scalaPbGrpc = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion

  // Project Dependencies
  lazy val rootDependencies: Seq[ModuleID] = Seq(scalaPbProto, scalaPbGrpc)

}
