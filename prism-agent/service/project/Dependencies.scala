import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.5"
    val zioConfig = "3.0.7"
    val zioHttp = "2.0.0-RC11"
    val zioInteropCats = "3.3.0" // scala-steward:off
    val akka = "2.6.20"
    val akkaHttp = "10.2.9"
    val castor = "0.5.1"
    val pollux = "0.15.0"
    val connect = "0.6.0"
    val bouncyCastle = "1.70"
    val logback = "1.4.5"
    val mercury = "0.15.0"
    val zioJson = "0.3.0"
    val tapir = "1.2.5"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio
  private lazy val zioConfig = "dev.zio" %% "zio-config" % Versions.zioConfig
  private lazy val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig
  private lazy val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig
  private lazy val zioJson = "dev.zio" %% "zio-json" % Versions.zioJson
  private lazy val zioInteropCats = "dev.zio" %% "zio-interop-cats" % Versions.zioInteropCats

  private lazy val zioTest = "dev.zio" %% "zio-test" % Versions.zio % Test
  private lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Versions.zio % Test
  private lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % Versions.zio % Test

  private lazy val zioHttp = "io.d11" %% "zhttp" % Versions.zioHttp

  private lazy val akkaTyped = "com.typesafe.akka" %% "akka-actor-typed" % Versions.akka
  private lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.akka
  private lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp
  private lazy val akkaSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % Versions.akkaHttp

  private lazy val castorCore = "io.iohk.atala" %% "castor-core" % Versions.castor
  private lazy val castorSqlDoobie = "io.iohk.atala" %% "castor-sql-doobie" % Versions.castor

  private lazy val polluxCore = "io.iohk.atala" %% "pollux-core" % Versions.pollux
  private lazy val polluxSqlDoobie = "io.iohk.atala" %% "pollux-sql-doobie" % Versions.pollux

  private lazy val connectCore = "io.iohk.atala" %% "connect-core" % Versions.connect
  private lazy val connectSqlDoobie = "io.iohk.atala" %% "connect-sql-doobie" % Versions.connect

  private lazy val mercuryAgent = "io.iohk.atala" %% "mercury-agent-didcommx" % Versions.mercury
  private lazy val mercuryPresentProof = "io.iohk.atala" %% "mercury-protocol-present-proof" % Versions.mercury

  // Added here to make prism-crypto works.
  // Once migrated to apollo, re-evaluate if this should be removed.
  private lazy val bouncyBcpkix = "org.bouncycastle" % "bcpkix-jdk15on" % Versions.bouncyCastle
  private lazy val bouncyBcprov = "org.bouncycastle" % "bcprov-jdk15on" % Versions.bouncyCastle

  private lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

  private lazy val tapirSwaggerUiBundle = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % Versions.tapir
  private lazy val tapirJsonZio = "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % Versions.tapir

  private lazy val tapirZioHttpServer = "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % Versions.tapir
  private lazy val tapirHttp4sServerZio = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % Versions.tapir
  private lazy val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % "0.23.12"

  private lazy val tapirRedocBundle = "com.softwaremill.sttp.tapir" %% "tapir-redoc-bundle" % Versions.tapir

  private lazy val tapirSttpStubServer =
    "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % Versions.tapir % Test
  private lazy val sttpClient3ZioJson = "com.softwaremill.sttp.client3" %% "zio-json" % "3.8.8" % Test

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] = Seq(
    zio,
    zioTest,
    zioTestSbt,
    zioTestMagnolia,
    zioConfig,
    zioConfigMagnolia,
    zioConfigTypesafe,
    zioJson,
    logback,
    zioHttp
  )
  private lazy val castorDependencies: Seq[ModuleID] = Seq(castorCore, castorSqlDoobie)
  private lazy val polluxDependencies: Seq[ModuleID] = Seq(polluxCore, polluxSqlDoobie)
  private lazy val mercuryDependencies: Seq[ModuleID] = Seq(mercuryAgent)
  private lazy val connectDependencies: Seq[ModuleID] = Seq(connectCore, connectSqlDoobie)
  private lazy val akkaHttpDependencies: Seq[ModuleID] =
    Seq(akkaTyped, akkaStream, akkaHttp, akkaSprayJson).map(_.cross(CrossVersion.for3Use2_13))
  private lazy val bouncyDependencies: Seq[ModuleID] = Seq(bouncyBcpkix, bouncyBcprov)
  private lazy val tapirDependencies: Seq[ModuleID] =
    Seq(
      tapirSwaggerUiBundle,
      tapirJsonZio,
      tapirRedocBundle,
      tapirSttpStubServer,
      sttpClient3ZioJson,
      tapirZioHttpServer,
      tapirHttp4sServerZio,
      http4sBlazeServer
    )

  // Project Dependencies
  lazy val keyManagementDependencies: Seq[ModuleID] =
    baseDependencies ++
      castorDependencies ++
      mercuryDependencies ++
      bouncyDependencies

  lazy val serverDependencies: Seq[ModuleID] =
    baseDependencies ++
      akkaHttpDependencies ++
      castorDependencies ++
      polluxDependencies ++
      mercuryDependencies ++
      connectDependencies ++
      tapirDependencies
}
