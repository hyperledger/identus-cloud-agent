import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.2"
    val akka = "2.6.19"
    val akkaHttp = "10.2.9"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio
  private lazy val zioStream = "dev.zio" %% "zio-streams" % Versions.zio

  private lazy val akkaTyped = "com.typesafe.akka" %% "akka-actor-typed" % Versions.akka
  private lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.akka
  private lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp
  private lazy val akkaSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % Versions.akkaHttp

  private lazy val grpcNetty = "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion
  private lazy val grpcServices = "io.grpc" % "grpc-services" % scalapb.compiler.Version.grpcJavaVersion
  private lazy val scalaPbProto = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  private lazy val scalaPbGrpc = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] = Seq(zio)
  private lazy val akkaHttpDependencies: Seq[ModuleID] = Seq(akkaTyped, akkaStream, akkaHttp, akkaSprayJson).map(_.cross(CrossVersion.for3Use2_13))
  private lazy val grpcDependencies: Seq[ModuleID] = Seq(grpcNetty, grpcServices, scalaPbProto, scalaPbGrpc)

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] = baseDependencies
  lazy val apiServerDependencies: Seq[ModuleID] = baseDependencies ++ akkaHttpDependencies ++ grpcDependencies
  lazy val workerDependencies: Seq[ModuleID] = baseDependencies ++ Seq(zioStream)
}
