import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.2"
    val akka = "2.6.19"
    val akkaHttp = "10.2.9"
    val logback = "1.2.11"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio

  private lazy val akkaTyped = "com.typesafe.akka" %% "akka-actor-typed" % Versions.akka
  private lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.akka
  private lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp
  private lazy val akkaSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % Versions.akkaHttp

  private lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback % Runtime

  // Dependencies
  lazy val baseDependencies: Seq[ModuleID] = Seq(zio)
  lazy val runtimeDependencies: Seq[ModuleID] = Seq(logback)
  lazy val akkaHttpDependencies: Seq[ModuleID] = Seq(akkaTyped, akkaStream, akkaHttp, akkaSprayJson).map(_.cross(CrossVersion.for3Use2_13))
}
