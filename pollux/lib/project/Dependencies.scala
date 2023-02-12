import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.4"
    val doobie = "1.0.0-RC2"
    val zioCatsInterop = "3.3.0"
    val prismSdk = "v1.4.1" // scala-steward:off
    val iris = "0.1.0"
    val shared = "0.2.0"
    val mercury = "0.18.0"
    val flyway = "9.8.3"
    val testContainersScalaPostgresql = "0.40.12"
    val quill = "4.6.0"
    val logback = "1.4.5"
    val munit = "1.0.0-M6" // "0.7.29"
    val munitZio = "0.1.1"
  }

  private lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback % Test
  private lazy val slf4jApi = "org.slf4j" % "slf4j-api" % "2.0.6" % Test
  private lazy val slf4jSimple = "org.slf4j" % "slf4j-simple" % "2.0.6" % Test

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio
  private lazy val zioCatsInterop = "dev.zio" %% "zio-interop-cats" % Versions.zioCatsInterop
  private lazy val zioTest = "dev.zio" %% "zio-test" % Versions.zio % Test
  private lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Versions.zio % Test
  private lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % Versions.zio % Test
  // For munit https://scalameta.org/munit/docs/getting-started.html
  private lazy val munit = "org.scalameta" %% "munit" % Versions.munit % Test
  // For munit zio https://github.com/poslegm/munit-zio
  private lazy val munitZio = "com.github.poslegm" %% "munit-zio" % Versions.munitZio % Test

  private lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % Versions.doobie
  private lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Versions.doobie

  private lazy val flyway = "org.flywaydb" % "flyway-core" % Versions.flyway

  private lazy val quillDoobie =
    "io.getquill" %% "quill-doobie" % Versions.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")
  private lazy val testcontainers =
    "com.dimafeng" %% "testcontainers-scala-postgresql" % Versions.testContainersScalaPostgresql % Test

  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  private lazy val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % Versions.prismSdk excludeAll
    ExclusionRule(
      organization = "org.bouncycastle"
    )

  private lazy val shared = "io.iohk.atala" % "shared" % Versions.shared
  private lazy val irisClient = "io.iohk.atala" %% "iris-client" % Versions.iris

  private lazy val mercuryProtocolIssueCredential =
    "io.iohk.atala" %% "mercury-protocol-issue-credential" % Versions.mercury
  private lazy val mercuryProtocolPresentProof =
    "io.iohk.atala" %% "mercury-protocol-present-proof" % Versions.mercury
  private lazy val mercuryResolver = "io.iohk.atala" %% "mercury-resolver" % Versions.mercury
  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] = Seq(
    zio,
    zioTest,
    zioTestSbt,
    zioTestMagnolia,
    munit,
    munitZio,
    shared,
    logback,
    slf4jApi,
    slf4jSimple
  )

  private lazy val doobieDependencies: Seq[ModuleID] = Seq(
    zioCatsInterop,
    doobiePostgres,
    doobieHikari,
    flyway,
    quillDoobie,
    testcontainers
  )

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] =
    baseDependencies ++ Seq(irisClient) ++ Seq(
      mercuryProtocolIssueCredential,
      mercuryProtocolPresentProof,
      mercuryResolver
    )
  lazy val sqlDoobieDependencies: Seq[ModuleID] = baseDependencies ++ doobieDependencies
}
