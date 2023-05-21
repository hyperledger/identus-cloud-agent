import sbtbuildinfo.BuildInfoPlugin.autoImport.*
import org.scoverage.coveralls.Imports.CoverallsKeys._

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.2",
    fork := true,
    run / connectInput := true,
    releaseUseGlobalVersion := false,
    versionScheme := Some("semver-spec"),
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks"
  )
)

coverageDataDir := target.value / "coverage"
coberturaFile := target.value / "coverage" / "coverage-report" / "cobertura.xml"

inThisBuild(
  Seq(
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-deprecation",
      "-unchecked",
      // TODO "-feature",
      // TODO "-Xfatal-warnings",
      // TODO "-Yexplicit-nulls",
      // "-Ysafe-init",
    )
  )
)

lazy val V = new {
  val munit = "1.0.0-M6" // "0.7.29"
  val munitZio = "0.1.1"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.0.13"
  val zioConfig = "3.0.2"
  val zioLogging = "2.0.0"
  val zioJson = "0.3.0"
  val zioHttp = "0.0.3"
  val zioCatsInterop = "3.3.0"
  val zioMetrics = "2.0.6"

  // https://mvnrepository.com/artifact/io.circe/circe-core
  val circe = "0.14.2"

  // val tapir = "1.0.3"
  val tapir = "1.2.13"

  val typesafeConfig = "1.4.2"
  val protobuf = "3.1.9"
  val testContainersScalaPostgresql = "0.40.15"

  val doobie = "1.0.0-RC2"
  val quill = "4.6.0"
  val iris = "0.1.0" // TODO REMOVE
  val flyway = "9.8.3"
  val logback = "1.4.7"

  val prismNodeClient = "0.4.0"
  val prismSdk = "v1.4.1" // scala-steward:off
  val scalaUri = "4.0.3"

  val circeVersion = "0.14.3"
  val jwtCirceVersion = "9.3.0"
  val zioPreludeVersion = "1.0.0-RC16"

  val bouncyCastle = "1.70"
}

/** Dependencies */
lazy val D = new {
  val zio: ModuleID = "dev.zio" %% "zio" % V.zio
  val zioStreams: ModuleID = "dev.zio" %% "zio-streams" % V.zio
  val zioLog: ModuleID = "dev.zio" %% "zio-logging" % V.zioLogging
  val zioSLF4J: ModuleID = "dev.zio" %% "zio-logging-slf4j" % V.zioLogging
  val zioJson: ModuleID = "dev.zio" %% "zio-json" % V.zioJson
  val zioHttp: ModuleID = "dev.zio" %% "zio-http" % V.zioHttp
  val zioCatsInterop: ModuleID = "dev.zio" %% "zio-interop-cats" % V.zioCatsInterop
  val zioMetrics: ModuleID = "dev.zio" %% "zio-metrics-connectors" % V.zioMetrics

  val zioConfig: ModuleID = "dev.zio" %% "zio-config" % V.zioConfig
  val zioConfigMagnolia: ModuleID = "dev.zio" %% "zio-config-magnolia" % V.zioConfig
  val zioConfigTypesafe: ModuleID = "dev.zio" %% "zio-config-typesafe" % V.zioConfig

  val circeCore: ModuleID = "io.circe" %% "circe-core" % V.circe
  val circeGeneric: ModuleID = "io.circe" %% "circe-generic" % V.circe
  val circeParser: ModuleID = "io.circe" %% "circe-parser" % V.circe

  // https://mvnrepository.com/artifact/org.didcommx/didcomm/0.3.2
  val didcommx: ModuleID = "org.didcommx" % "didcomm" % "0.3.1"
  val peerDidcommx: ModuleID = "org.didcommx" % "peerdid" % "0.3.0"
  val didScala: ModuleID = "app.fmgp" %% "did" % "0.0.0+113-61efa271-SNAPSHOT"

  // https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt/9.16-preview.1
  val jwk: ModuleID = "com.nimbusds" % "nimbus-jose-jwt" % "9.25.6"

  val typesafeConfig: ModuleID = "com.typesafe" % "config" % V.typesafeConfig
  val scalaPbGrpc: ModuleID = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
  // TODO we are adding test stuff to the main dependencies
  val testcontainers: ModuleID = "com.dimafeng" %% "testcontainers-scala-postgresql" % V.testContainersScalaPostgresql

  val doobiePostgres: ModuleID = "org.tpolecat" %% "doobie-postgres" % V.doobie
  val doobieHikari: ModuleID = "org.tpolecat" %% "doobie-hikari" % V.doobie
  val flyway: ModuleID = "org.flywaydb" % "flyway-core" % V.flyway

  // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit: ModuleID = "org.scalameta" %% "munit" % V.munit % Test
  // For munit zio https://github.com/poslegm/munit-zio
  val munitZio: ModuleID = "com.github.poslegm" %% "munit-zio" % V.munitZio % Test

  val zioTest: ModuleID = "dev.zio" %% "zio-test" % V.zio % Test
  val zioTestSbt: ModuleID = "dev.zio" %% "zio-test-sbt" % V.zio % Test
  val zioTestMagnolia: ModuleID = "dev.zio" %% "zio-test-magnolia" % V.zio % Test

  // LIST of Dependencies
  val doobieDependencies: Seq[ModuleID] =
    Seq(doobiePostgres, doobieHikari, flyway)
}

lazy val D_Shared = new {

  lazy val dependencies: Seq[ModuleID] = Seq(D.typesafeConfig, D.scalaPbGrpc, D.testcontainers)
}

lazy val D_Connect = new {

  private lazy val logback = "ch.qos.logback" % "logback-classic" % V.logback % Test

  private lazy val testcontainers =
    "com.dimafeng" %% "testcontainers-scala-postgresql" % V.testContainersScalaPostgresql % Test

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] =
    Seq(D.zio, D.zioTest, D.zioTestSbt, D.zioTestMagnolia, testcontainers, logback)

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] =
    baseDependencies
  lazy val sqlDoobieDependencies: Seq[ModuleID] =
    baseDependencies ++ D.doobieDependencies ++ Seq(D.zioCatsInterop)
}

lazy val D_Castor = new {

  val scalaUri = "io.lemonlabs" %% "scala-uri" % V.scalaUri
  val prismNodeClient = "io.iohk.atala" %% "prism-node-client" % V.prismNodeClient

  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % V.prismSdk excludeAll
    ExclusionRule(
      organization = "org.bouncycastle"
    )
  val prismIdentity = "io.iohk.atala" % "prism-identity-jvm" % V.prismSdk

  // Dependency Modules
  val baseDependencies: Seq[ModuleID] =
    Seq(
      D.zio,
      D.zioTest,
      D.zioTestSbt,
      D.zioTestMagnolia,
      prismCrypto,
      prismIdentity,
      prismNodeClient,
      scalaUri
    )

  // Project Dependencies
  val coreDependencies: Seq[ModuleID] = baseDependencies
}

lazy val D_Pollux = new {
  val logback = "ch.qos.logback" % "logback-classic" % V.logback % Test
  val slf4jApi = "org.slf4j" % "slf4j-api" % "2.0.6" % Test
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % "2.0.6" % Test

  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % V.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % V.doobie

  val flyway = "org.flywaydb" % "flyway-core" % V.flyway

  val quillJdbcZio = "io.getquill" %% "quill-jdbc-zio" %
    V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")

  val quillDoobie = "io.getquill" %% "quill-doobie" %
    V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")

  val testcontainers =
    "com.dimafeng" %% "testcontainers-scala-postgresql" % V.testContainersScalaPostgresql % Test

  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % V.prismSdk excludeAll
    ExclusionRule(
      organization = "org.bouncycastle"
    )

  val irisClient = "io.iohk.atala" %% "iris-client" % V.iris // TODO REMOVE?

  // Dependency Modules
  val baseDependencies: Seq[ModuleID] = Seq(
    D.zio,
    D.zioJson,
    D.zioTest,
    D.zioTestSbt,
    D.zioTestMagnolia,
    D.munit,
    D.munitZio,
    prismCrypto,
    // shared,
    logback,
    slf4jApi,
    slf4jSimple
  )

  val doobieDependencies: Seq[ModuleID] = Seq(
    D.zioCatsInterop,
    D.doobiePostgres,
    D.doobieHikari,
    flyway,
    quillDoobie,
    quillJdbcZio,
    testcontainers
  )

  // Project Dependencies
  val coreDependencies: Seq[ModuleID] = baseDependencies ++ Seq(irisClient)
  val sqlDoobieDependencies: Seq[ModuleID] = baseDependencies ++ doobieDependencies
}

lazy val D_Pollux_VC_JWT = new {

  private lazy val circeJsonSchema = ("net.reactivecore" %% "circe-json-schema" % "0.3.0")
    .cross(CrossVersion.for3Use2_13)
    .exclude("io.circe", "circe-core_2.13")
    .exclude("io.circe", "circe-generic_2.13")
    .exclude("io.circe", "circe-parser_2.13")

  val jwtCirce = "com.github.jwt-scala" %% "jwt-circe" % V.jwtCirceVersion

  val zio = "dev.zio" %% "zio" % V.zio
  val zioPrelude = "dev.zio" %% "zio-prelude" % V.zioPreludeVersion

  val nimbusJoseJwt = "com.nimbusds" % "nimbus-jose-jwt" % "10.0.0-preview"

  val zioTest = "dev.zio" %% "zio-test" % V.zio % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio % Test
  val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % V.zio % Test

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.9" % Test

  // Dependency Modules
  val zioDependencies: Seq[ModuleID] = Seq(zio, zioPrelude, zioTest, zioTestSbt, zioTestMagnolia)
  val circeDependencies: Seq[ModuleID] = Seq(D.circeCore, D.circeGeneric, D.circeParser)
  val baseDependencies: Seq[ModuleID] =
    circeDependencies ++ zioDependencies :+ jwtCirce :+ circeJsonSchema :+ nimbusJoseJwt :+ scalaTest

  // Project Dependencies
  lazy val polluxVcJwtDependencies: Seq[ModuleID] = baseDependencies
}

lazy val D_PrismAgent = new {

  // Added here to make prism-crypto works.
  // Once migrated to apollo, re-evaluate if this should be removed.
  val bouncyBcpkix = "org.bouncycastle" % "bcpkix-jdk15on" % V.bouncyCastle
  val bouncyBcprov = "org.bouncycastle" % "bcprov-jdk15on" % V.bouncyCastle

  val logback = "ch.qos.logback" % "logback-classic" % V.logback

  val tapirSwaggerUiBundle = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % V.tapir
  val tapirJsonZio = "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % V.tapir

  val tapirZioHttpServer = "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % V.tapir
  val tapirHttp4sServerZio = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % V.tapir
  val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % "0.23.12"

  val tapirRedocBundle = "com.softwaremill.sttp.tapir" %% "tapir-redoc-bundle" % V.tapir

  val tapirSttpStubServer =
    "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % V.tapir % Test
  val sttpClient3ZioJson = "com.softwaremill.sttp.client3" %% "zio-json" % "3.8.15" % Test

  val quillDoobie =
    "io.getquill" %% "quill-doobie" % V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")
  val postgresql = "org.postgresql" % "postgresql" % "42.2.8"
  val quillJdbcZio =
    "io.getquill" %% "quill-jdbc-zio" % V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")

  val flyway = "org.flywaydb" % "flyway-core" % V.flyway
  val testcontainers_scala_postgresql =
    "com.dimafeng" %% "testcontainers-scala-postgresql" % V.testContainersScalaPostgresql % Test

  // Dependency Modules
  val baseDependencies: Seq[ModuleID] = Seq(
    D.zio,
    D.zioTest,
    D.zioTestSbt,
    D.zioTestMagnolia,
    D.zioConfig,
    D.zioConfigMagnolia,
    D.zioConfigTypesafe,
    D.zioJson,
    logback,
    D.zioHttp,
    D.zioMetrics,
  )
  val bouncyDependencies: Seq[ModuleID] = Seq(bouncyBcpkix, bouncyBcprov)
  val tapirDependencies: Seq[ModuleID] =
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

  val postgresDependencies: Seq[ModuleID] =
    Seq(quillDoobie, quillJdbcZio, postgresql, flyway, testcontainers_scala_postgresql)

  // Project Dependencies
  lazy val keyManagementDependencies: Seq[ModuleID] =
    baseDependencies ++ bouncyDependencies ++ D.doobieDependencies ++ Seq(D.zioCatsInterop)

  lazy val serverDependencies: Seq[ModuleID] =
    baseDependencies ++ tapirDependencies ++ postgresDependencies
}

publish / skip := true

// #####################
// #####  shared  ######
// #####################

lazy val shared = (project in file("shared"))
  // .configure(publishConfigure)
  .settings(
    organization := "io.iohk.atala",
    organizationName := "Input Output Global",
    buildInfoPackage := "io.iohk.atala.shared",
    name := "shared",
    crossPaths := false,
    libraryDependencies ++= D_Shared.dependencies
  )
  .enablePlugins(BuildInfoPlugin)

// #########################
// ### Models & Services ###
// #########################

/** Just data models and interfaces of service.
  *
  * This module must not depend on external libraries!
  */
lazy val models = project
  .in(file("mercury/mercury-library/models"))
  .settings(name := "mercury-data-models")
  .settings(
    libraryDependencies ++= Seq(D.zio),
    libraryDependencies ++= Seq(
      D.circeCore,
      D.circeGeneric,
      D.circeParser
    ), // TODO try to remove this from this module
    // libraryDependencies += D.didScala
  )
  .settings(libraryDependencies += D.jwk) //FIXME just for the DidAgent

/* TODO move code from agentDidcommx to here
models implementation for didcommx () */
// lazy val modelsDidcommx = project
//   .in(file("models-didcommx"))
//   .settings(name := "mercury-models-didcommx")
//   .settings(libraryDependencies += D.didcommx)
//   .dependsOn(models)

// #################
// ### Protocols ###
// #################

lazy val protocolConnection = project
  .in(file("mercury/mercury-library/protocol-connection"))
  .settings(name := "mercury-protocol-connection")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models, protocolInvitation)

lazy val protocolCoordinateMediation = project
  .in(file("mercury/mercury-library/protocol-coordinate-mediation"))
  .settings(name := "mercury-protocol-coordinate-mediation")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolDidExchange = project
  .in(file("mercury/mercury-library/protocol-did-exchange"))
  .settings(name := "mercury-protocol-did-exchange")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .dependsOn(models, protocolInvitation)

lazy val protocolInvitation = project
  .in(file("mercury/mercury-library/protocol-invitation"))
  .settings(name := "mercury-protocol-invitation")
  .settings(libraryDependencies += D.zio)
  .settings(
    libraryDependencies ++= Seq(
      D.circeCore,
      D.circeGeneric,
      D.circeParser,
      D.munit,
      D.munitZio
    )
  )
  .dependsOn(models)

lazy val protocolMercuryMailbox = project
  .in(file("mercury/mercury-library/protocol-mercury-mailbox"))
  .settings(name := "mercury-protocol-mailbox")
  .settings(libraryDependencies += D.zio)
  .dependsOn(models, protocolInvitation, protocolRouting)

lazy val protocolLogin = project
  .in(file("mercury/mercury-library/protocol-outofband-login"))
  .settings(name := "mercury-protocol-outofband-login")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolReportProblem = project
  .in(file("mercury/mercury-library/protocol-report-problem"))
  .settings(name := "mercury-protocol-report-problem")
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolRouting = project
  .in(file("mercury/mercury-library/protocol-routing"))
  .settings(name := "mercury-protocol-routing-2-0")
  .settings(libraryDependencies += D.zio)
  .dependsOn(models)

lazy val protocolIssueCredential = project
  .in(file("mercury/mercury-library/protocol-issue-credential"))
  .settings(name := "mercury-protocol-issue-credential")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolPresentProof = project
  .in(file("mercury/mercury-library/protocol-present-proof"))
  .settings(name := "mercury-protocol-present-proof")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolTrustPing = project
  .in(file("mercury/mercury-library/protocol-trust-ping"))
  .settings(name := "mercury-protocol-trust-ping")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

// ################
// ### Resolver ###
// ################

// TODO move stuff to the models module
lazy val resolver = project // maybe merge into models
  .in(file("mercury/mercury-library/resolver"))
  .settings(name := "mercury-resolver")
  .settings(
    libraryDependencies ++= Seq(
      D.didcommx,
      D.peerDidcommx,
      D.munit,
      D.munitZio,
      D.jwk,
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(models)

// ##############
// ### Agents ###
// ##############

lazy val agent = project // maybe merge into models
  .in(file("mercury/mercury-library/agent"))
  .settings(name := "mercury-agent-core")
  .settings(libraryDependencies ++= Seq(D.zioLog)) // , D.zioSLF4J))
  .dependsOn(
    models,
    resolver,
    protocolCoordinateMediation,
    protocolInvitation,
    protocolRouting,
    protocolMercuryMailbox,
    protocolLogin,
    protocolIssueCredential,
    protocolPresentProof,
    protocolConnection,
    protocolReportProblem,
    protocolTrustPing,
  )

/** agents implementation with didcommx */
lazy val agentDidcommx = project
  .in(file("mercury/mercury-library/agent-didcommx"))
  .settings(name := "mercury-agent-didcommx")
  .settings(libraryDependencies += D.didcommx)
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(agent) //modelsDidcommx

/** Demos agents and services implementation with didcommx */
lazy val agentCliDidcommx = project
  .in(file("mercury/mercury-library/agent-cli-didcommx"))
  .settings(name := "mercury-agent-cli-didcommx")
  .settings(libraryDependencies += "com.google.zxing" % "core" % "3.5.1")
  .settings(libraryDependencies += D.zioHttp)
  .dependsOn(agentDidcommx)

// ///** TODO Demos agents and services implementation with did-scala */
// lazy val agentDidScala =
//   project
//     .in(file("mercury/mercury-library/agent-did-scala"))
//     .settings(name := "mercury-agent-didscala")
//     .settings(skip / publish := true)
//     .dependsOn(agent)

// #####################
// #####  castor  ######
// #####################

val castorCommonSettings = Seq(
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  githubTokenSource := TokenSource.Environment("GITHUB_TOKEN"),
  resolvers += Resolver.githubPackages("input-output-hk"),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven"
)

// Project definitions
lazy val castorCore = project
  .in(file("castor/lib/core"))
  .settings(castorCommonSettings)
  .settings(
    name := "castor-core",
    libraryDependencies ++= D_Castor.coreDependencies
  )
  .dependsOn(shared)

// #####################
// #####  pollux  ######
// #####################

val polluxCommonSettings = Seq(
  testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  githubTokenSource := TokenSource.Environment("GITHUB_TOKEN"),
  resolvers += Resolver.githubPackages("input-output-hk"),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven"
)

lazy val polluxVcJWT = project
  .in(file("pollux/lib/vc-jwt"))
  .settings(polluxCommonSettings)
  .settings(
    name := "pollux-vc-jwt",
    libraryDependencies ++= D_Pollux_VC_JWT.polluxVcJwtDependencies
  )
  .dependsOn(castorCore)

lazy val polluxCore = project
  .in(file("pollux/lib/core"))
  .settings(polluxCommonSettings)
  .settings(
    name := "pollux-core",
    libraryDependencies ++= D_Pollux.coreDependencies
  )
  .dependsOn(shared)
  .dependsOn(polluxVcJWT)
  .dependsOn(protocolIssueCredential, protocolPresentProof, resolver, agentDidcommx)

lazy val polluxDoobie = project
  .in(file("pollux/lib/sql-doobie"))
  .settings(polluxCommonSettings)
  .settings(
    name := "pollux-sql-doobie",
    libraryDependencies ++= D_Pollux.sqlDoobieDependencies
  )
  .dependsOn(polluxCore % "compile->compile;test->test")
  .dependsOn(shared)

// #####################
// #####  connect  #####
// #####################

def connectCommonSettings = polluxCommonSettings

lazy val connectCore = project
  .in(file("connect/lib/core"))
  .settings(connectCommonSettings)
  .settings(
    name := "connect-core",
    libraryDependencies ++= D_Connect.coreDependencies,
    Test / publishArtifact := true
  )
  .dependsOn(shared)
  .dependsOn(protocolConnection, protocolReportProblem)

lazy val connectDoobie = project
  .in(file("connect/lib/sql-doobie"))
  .settings(connectCommonSettings)
  .settings(
    name := "connect-sql-doobie",
    libraryDependencies ++= D_Connect.sqlDoobieDependencies
  )
  .dependsOn(shared)
  .dependsOn(connectCore % "compile->compile;test->test")

// #####################
// #### Prism Agent ####
// #####################
def prismAgentConnectCommonSettings = polluxCommonSettings

lazy val prismAgentWalletAPI = project
  .in(file("prism-agent/service/wallet-api"))
  .settings(prismAgentConnectCommonSettings)
  .settings(
    name := "prism-agent-wallet-api",
    libraryDependencies ++= D_PrismAgent.keyManagementDependencies
  )
  .dependsOn(agentDidcommx)
  .dependsOn(castorCore)

lazy val prismAgentServer = project
  .in(file("prism-agent/service/server"))
  .settings(prismAgentConnectCommonSettings)
  .settings(
    name := "prism-agent",
    fork := true,
    libraryDependencies ++= D_PrismAgent.serverDependencies,
    Compile / mainClass := Some("io.iohk.atala.agent.server.MainApp"),
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerUsername := Some("input-output-hk"),
    Docker / dockerRepository := Some("ghcr.io"),
    dockerExposedPorts := Seq(8080, 8085, 8090),
    dockerBaseImage := "openjdk:11",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.iohk.atala.agent.server.buildinfo"
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(prismAgentWalletAPI)
  .dependsOn(
    agent,
    polluxCore,
    polluxDoobie,
    connectCore,
    connectDoobie,
    castorCore
  )

// ##################
// #### Mediator ####
// ##################

/** The mediator service */
lazy val mediator = project
  .in(file("mercury/mercury-mediator"))
  .settings(name := "mercury-mediator")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies += D.zioHttp)
  .settings(libraryDependencies += D.munitZio)
  .settings(
    // Compile / unmanagedResourceDirectories += apiBaseDirectory.value,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    // ### Build Docker Image ###
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerRepository := Some("ghcr.io"),
    Docker / dockerUsername := Some("input-output-hk"),
    // Docker / githubOwner := "atala-prism-building-blocks",
    // Docker / dockerUpdateLatest := true,
    dockerExposedPorts := Seq(8080),
    dockerBaseImage := "openjdk:11"
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(models, agentDidcommx)

// ############################
// ####  Release process  #####
// ############################
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations.*
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  ReleaseStep(releaseStepTask(prismAgentServer / Docker / stage)),
  sys.env
    .get("RELEASE_MEDIATOR") match {
    case Some(value) => ReleaseStep(releaseStepTask(mediator / Docker / stage))
    case None =>
      ReleaseStep(action = st => {
        println("INFO: prism mediator release disabled!")
        st
      })
  },
  setNextVersion
)
