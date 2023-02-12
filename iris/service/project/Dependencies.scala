import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.8"
    val zioConfig = "3.0.7"
    val circe = "0.14.1"
    val circeOptics = "0.14.1"
    val akka = "2.6.19"
    val doobie = "1.0.0-RC2"
    val zioCatsInterop = "3.3.0"
    val prismSdk = "v1.4.1" // scala-steward:off
    val shared = "0.1.0"
    val enumeratum = "1.7.2"
    val zioTest = "2.0.4"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio
  private lazy val zioConfig = "dev.zio" %% "zio-config" % Versions.zioConfig
  private lazy val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig
  private lazy val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig
  private lazy val zioStream = "dev.zio" %% "zio-streams" % Versions.zio
  private lazy val zioCatsInterop = "dev.zio" %% "zio-interop-cats" % Versions.zioCatsInterop
  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  private lazy val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % Versions.prismSdk excludeAll
    ExclusionRule(
      organization = "org.bouncycastle"
    )
  private lazy val shared = "io.iohk.atala" % "shared" % Versions.shared
  private lazy val enumeratum = ("com.beachape" %% "enumeratum" % Versions.enumeratum).cross(CrossVersion.for3Use2_13)

  private lazy val grpcNetty = "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion
  private lazy val grpcServices = "io.grpc" % "grpc-services" % scalapb.compiler.Version.grpcJavaVersion
  private lazy val scalaPbProto =
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  private lazy val scalaPbGrpc =
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion

  private lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % Versions.doobie
  private lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Versions.doobie

  val circeCore = "io.circe" %% "circe-core" % Versions.circe
  val circeGeneric = "io.circe" %% "circe-generic" % Versions.circe
//  val circeGenericExtras = ("io.circe" %% "circe-generic-extras" % Versions.circe).cross(CrossVersion.for3Use2_13)
  val circeParser = "io.circe" %% "circe-parser" % Versions.circe
//  val circeOptics = ("io.circe" %% "circe-optics" % Versions.circeOptics).cross(CrossVersion.for3Use2_13)

  // Tests
  private lazy val zioTest = "dev.zio" %% "zio-test" % "2.0.4" % Test
  private lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % "2.0.4" % Test
  private lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % "2.0.4" % Test

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] =
    Seq(zio, zioConfig, zioConfigMagnolia, zioConfigTypesafe, zioStream, prismCrypto, shared, enumeratum)
  private lazy val grpcDependencies: Seq[ModuleID] = Seq(grpcNetty, grpcServices, scalaPbProto, scalaPbGrpc)
  private lazy val doobieDependencies: Seq[ModuleID] = Seq(doobiePostgres, doobieHikari)
  private lazy val circeDependencies: Seq[ModuleID] =
    Seq(circeCore, circeGeneric, circeParser)
  private lazy val zioTestDependencies: Seq[ModuleID] = Seq(zioTest, zioTestSbt, zioTestMagnolia, zioCatsInterop)

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] =
    baseDependencies ++ grpcDependencies ++ circeDependencies ++ zioTestDependencies
  lazy val sqlDependencies: Seq[ModuleID] = baseDependencies ++ doobieDependencies ++ Seq(zioCatsInterop)
  lazy val serverDependencies: Seq[ModuleID] = baseDependencies
}
