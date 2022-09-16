import sbt._

object Dependencies {

  private lazy val scalaPbProto = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  private lazy val scalaPbGrpc = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion

  private lazy val grpcDependencies: Seq[ModuleID] = Seq(scalaPbProto, scalaPbGrpc)

  lazy val clientDependencies: Seq[ModuleID] = grpcDependencies

}
