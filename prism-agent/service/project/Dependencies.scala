import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.2"
    val akka = "2.6.19"
    val akkaHttp = "10.2.9"
    val castor = "0.1.0"
    val pollux = "0.1.0"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio

  private lazy val akkaTyped = "com.typesafe.akka" %% "akka-actor-typed" % Versions.akka
  private lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.akka
  private lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp
  private lazy val akkaSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % Versions.akkaHttp

  private lazy val castorCore = "io.iohk.atala" %% "castor-core" % Versions.castor
  private lazy val castorSqlDoobie = "io.iohk.atala" %% "castor-sql-doobie" % Versions.castor

  private lazy val polluxCore = "io.iohk.atala" %% "pollux-core" % Versions.pollux
  private lazy val polluxSqlDoobie = "io.iohk.atala" %% "pollux-sql-doobie" % Versions.pollux

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] = Seq(zio)
  private lazy val castorDependencies: Seq[ModuleID] = Seq(castorCore, castorSqlDoobie)
  private lazy val polluxDependencies: Seq[ModuleID] = Seq(polluxCore, polluxSqlDoobie)
  private lazy val akkaHttpDependencies: Seq[ModuleID] = Seq(akkaTyped, akkaStream, akkaHttp, akkaSprayJson).map(_.cross(CrossVersion.for3Use2_13))

  // Project Dependencies
  lazy val custodianDependencies: Seq[ModuleID] = baseDependencies ++ castorDependencies
  lazy val serverDependencies: Seq[ModuleID] = baseDependencies ++ akkaHttpDependencies ++ castorDependencies ++ polluxDependencies
}
