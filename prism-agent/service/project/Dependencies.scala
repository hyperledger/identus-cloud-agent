import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.2"
    val zioConfig = "3.0.2"
    val akka = "2.6.20"
    val akkaHttp = "10.2.9"
    val castor = "0.1.0"
    val pollux = "0.1.0"
    val bouncyCastle = "1.70"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio
  private lazy val zioConfig = "dev.zio" %% "zio-config" % Versions.zioConfig
  private lazy val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig
  private lazy val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig

  private lazy val zioTest = "dev.zio" %% "zio-test" % Versions.zio % Test
  private lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Versions.zio % Test
  private lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % Versions.zio % Test

  private lazy val akkaTyped = "com.typesafe.akka" %% "akka-actor-typed" % Versions.akka
  private lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.akka
  private lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp
  private lazy val akkaSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % Versions.akkaHttp

  private lazy val castorCore = "io.iohk.atala" %% "castor-core" % Versions.castor
  private lazy val castorSqlDoobie = "io.iohk.atala" %% "castor-sql-doobie" % Versions.castor

  private lazy val polluxCore = "io.iohk.atala" %% "pollux-core" % Versions.pollux
  private lazy val polluxSqlDoobie = "io.iohk.atala" %% "pollux-sql-doobie" % Versions.pollux

  // Added here to make prism-crypto works.
  // Once migrated to apollo, re-evaluate if this should be removed.
  private lazy val bouncyBcpkix = "org.bouncycastle" % "bcpkix-jdk15on" % Versions.bouncyCastle
  private lazy val bouncyBcprov = "org.bouncycastle" % "bcprov-jdk15on" % Versions.bouncyCastle

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] = Seq(zio, zioTest, zioTestSbt, zioTestMagnolia, zioConfig, zioConfigMagnolia, zioConfigTypesafe)
  private lazy val castorDependencies: Seq[ModuleID] = Seq(castorCore, castorSqlDoobie)
  private lazy val polluxDependencies: Seq[ModuleID] = Seq(polluxCore, polluxSqlDoobie)
  private lazy val akkaHttpDependencies: Seq[ModuleID] = Seq(akkaTyped, akkaStream, akkaHttp, akkaSprayJson).map(_.cross(CrossVersion.for3Use2_13))
  private lazy val bouncyDependencies: Seq[ModuleID] = Seq(bouncyBcpkix, bouncyBcprov)

  // Project Dependencies
  lazy val keyManagementDependencies: Seq[ModuleID] = baseDependencies ++ castorDependencies ++ bouncyDependencies
  lazy val serverDependencies: Seq[ModuleID] = baseDependencies ++ akkaHttpDependencies ++ castorDependencies ++ polluxDependencies
}
