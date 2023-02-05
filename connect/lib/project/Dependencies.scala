import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.6"
    val doobie = "1.0.0-RC2"
    val zioCatsInterop = "23.0.0.1"
    val iris = "0.1.0"
    val mercury = "0.17.0"
    val flyway = "9.8.3"
    val shared = "0.2.0"
    val testContainersScalaPostgresql = "0.40.12"
    val logback = "1.4.5"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio
  private lazy val zioCatsInterop = "dev.zio" %% "zio-interop-cats" % Versions.zioCatsInterop

  private lazy val zioTest = "dev.zio" %% "zio-test" % Versions.zio % Test
  private lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Versions.zio % Test
  private lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % Versions.zio % Test

  private lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback % Test

  private lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % Versions.doobie
  private lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Versions.doobie

  private lazy val flyway = "org.flywaydb" % "flyway-core" % Versions.flyway

  private lazy val mercuryProtocolConnection = "io.iohk.atala" %% "mercury-protocol-connection" % Versions.mercury

  private lazy val shared = "io.iohk.atala" % "shared" % Versions.shared

  private lazy val testcontainers =
    "com.dimafeng" %% "testcontainers-scala-postgresql" % Versions.testContainersScalaPostgresql % Test

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] =
    Seq(zio, zioTest, zioTestSbt, zioTestMagnolia, shared, testcontainers, logback)
  private lazy val doobieDependencies: Seq[ModuleID] = Seq(doobiePostgres, doobieHikari, flyway)

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] =
    baseDependencies ++ Seq(mercuryProtocolConnection)
  lazy val sqlDoobieDependencies: Seq[ModuleID] = baseDependencies ++ doobieDependencies ++ Seq(zioCatsInterop)
}
