import sbtbuildinfo.BuildInfoPlugin.autoImport._

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
  val zio = "2.0.4"
  val zioConfig = "3.0.2"
  val zioLogging = "2.0.0"
  val zioJson = "0.3.0"
  val zioHttp = "2.0.0-RC11"
  val zioCatsInterop = "3.3.0"
  val zioMetrics = "2.0.6"

  // https://mvnrepository.com/artifact/io.circe/circe-core
  val circe = "0.14.2"

  // val tapir = "1.0.3"
  val tapir = "1.2.3"

  val typesafeConfig = "1.4.2"
  val protobuf = "3.1.9"
  val testContainersScalaPostgresql = "0.40.11"

  val doobie = "1.0.0-RC2"
  val quill = "4.6.0"
  val iris = "0.1.0" // TODO REMOVE
  val flyway = "9.8.3"
  val logback = "1.4.5"

  val prismNodeClient = "0.3.0"
  val prismSdk = "v1.4.1" // scala-steward:off
  val scalaUri = "4.0.3"

  val circeVersion = "0.14.3"
  val jwtCirceVersion = "9.1.2"
  val zioPreludeVersion = "1.0.0-RC16"

  val akka = "2.6.20"
  val akkaHttp = "10.2.9"
  val bouncyCastle = "1.70"
}

/** Dependencies */
lazy val D = new {
  val zio: ModuleID = "dev.zio" %% "zio" % V.zio
  val zioStreams: ModuleID = "dev.zio" %% "zio-streams" % V.zio
  val zioLog: ModuleID = "dev.zio" %% "zio-logging" % V.zioLogging
  val zioSLF4J: ModuleID = "dev.zio" %% "zio-logging-slf4j" % V.zioLogging
  val zioJson: ModuleID = "dev.zio" %% "zio-json" % V.zioJson
  val zioHttp: ModuleID = "dev.zio" %% "zio-http" % "0.0.3"
  val zioCatsInterop = "dev.zio" %% "zio-interop-cats" % V.zioCatsInterop

  val circeCore: ModuleID = "io.circe" %% "circe-core" % V.circe
  val circeGeneric: ModuleID = "io.circe" %% "circe-generic" % V.circe
  val circeParser: ModuleID = "io.circe" %% "circe-parser" % V.circe

  // https://mvnrepository.com/artifact/org.didcommx/didcomm/0.3.2
  val didcommx: ModuleID = "org.didcommx" % "didcomm" % "0.3.1"
  val peerDidcommx: ModuleID = "org.didcommx" % "peerdid" % "0.3.0"
  val didScala: ModuleID = "app.fmgp" %% "did" % "0.0.0+113-61efa271-SNAPSHOT"

  // https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt/9.16-preview.1
  val jwk: ModuleID = "com.nimbusds" % "nimbus-jose-jwt" % "9.25.4"

  val typesafeConfig = "com.typesafe" % "config" % V.typesafeConfig
  val scalaPbGrpc = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
  // TODO we are adding test stuff to the main dependencies
  val testcontainers = "com.dimafeng" %% "testcontainers-scala-postgresql" % V.testContainersScalaPostgresql

  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % V.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % V.doobie
  val flyway = "org.flywaydb" % "flyway-core" % V.flyway

  // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit = "org.scalameta" %% "munit" % V.munit % Test
  // For munit zio https://github.com/poslegm/munit-zio
  val munitZio = "com.github.poslegm" %% "munit-zio" % V.munitZio % Test

  val zioTest = "dev.zio" %% "zio-test" % V.zio % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio % Test
  val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % V.zio % Test

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

  lazy val scalaUri = "io.lemonlabs" %% "scala-uri" % V.scalaUri

  //  lazy val shared = "io.iohk.atala" % "shared" % V.shared
  lazy val prismNodeClient = "io.iohk.atala" %% "prism-node-client" % V.prismNodeClient

  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  lazy val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % V.prismSdk excludeAll
    ExclusionRule(
      organization = "org.bouncycastle"
    )
  lazy val prismIdentity = "io.iohk.atala" % "prism-identity-jvm" % V.prismSdk

  // Dependency Modules
  lazy val baseDependencies: Seq[ModuleID] =
    Seq(
      D.zio,
      D.zioTest,
      D.zioTestSbt,
      D.zioTestMagnolia,
      /*shared,*/
      prismCrypto,
      prismIdentity,
      prismNodeClient,
      scalaUri
    )

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] = baseDependencies
  lazy val sqlDoobieDependencies: Seq[ModuleID] =
    baseDependencies ++ D.doobieDependencies ++ Seq(D.zioCatsInterop)
}

lazy val D_Pollux = new {
  lazy val logback = "ch.qos.logback" % "logback-classic" % V.logback % Test
  lazy val slf4jApi = "org.slf4j" % "slf4j-api" % "2.0.6" % Test
  lazy val slf4jSimple = "org.slf4j" % "slf4j-simple" % "2.0.6" % Test

  // lazy val zio = "dev.zio" %% "zio" % V.zio
  // lazy val zioJson = "dev.zio" %% "zio-json" % V.zioJson
  // lazy val zioCatsInterop = "dev.zio" %% "zio-interop-cats" % V.zioCatsInterop
  // lazy val zioTest = "dev.zio" %% "zio-test" % V.zio % Test
  // lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio % Test
  // lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % V.zio % Test
  // For munit https://scalameta.org/munit/docs/getting-started.html
  lazy val munit = "org.scalameta" %% "munit" % V.munit % Test
  // For munit zio https://github.com/poslegm/munit-zio
  lazy val munitZio = "com.github.poslegm" %% "munit-zio" % V.munitZio % Test

  lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % V.doobie
  lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % V.doobie

  lazy val flyway = "org.flywaydb" % "flyway-core" % V.flyway

  lazy val quillJdbcZio = "io.getquill" %% "quill-jdbc-zio" %
    V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")

  lazy val quillDoobie = "io.getquill" %% "quill-doobie" %
    V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")

  lazy val testcontainers =
    "com.dimafeng" %% "testcontainers-scala-postgresql" % V.testContainersScalaPostgresql % Test

  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  lazy val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % V.prismSdk excludeAll
    ExclusionRule(
      organization = "org.bouncycastle"
    )

  //  lazy val shared = "io.iohk.atala" % "shared" % V.shared
  lazy val irisClient = "io.iohk.atala" %% "iris-client" % V.iris

  //  lazy val mercuryProtocolIssueCredential =
  //   "io.iohk.atala" %% "mercury-protocol-issue-credential" % V.mercury
  //  lazy val mercuryProtocolPresentProof =
  //   "io.iohk.atala" %% "mercury-protocol-present-proof" % V.mercury
  //  lazy val mercuryResolver = "io.iohk.atala" %% "mercury-resolver" % V.mercury
  // Dependency Modules
  lazy val baseDependencies: Seq[ModuleID] = Seq(
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

  lazy val doobieDependencies: Seq[ModuleID] = Seq(
    D.zioCatsInterop,
    D.doobiePostgres,
    D.doobieHikari,
    flyway,
    quillDoobie,
    quillJdbcZio,
    testcontainers
  )

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] =
    baseDependencies ++ Seq(irisClient)
    // ++ Seq(
    //   mercuryProtocolIssueCredential,
    //   mercuryProtocolPresentProof,
    //   mercuryResolver
    // )
  lazy val sqlDoobieDependencies: Seq[ModuleID] = baseDependencies ++ doobieDependencies
}

lazy val D_Pollux_VC_JWT = new {

  private lazy val coreJwtCirce = "io.circe" %% "circe-core" % V.circeVersion
  private lazy val genericJwtCirce = "io.circe" %% "circe-generic" % V.circeVersion
  private lazy val parserJwtCirce = "io.circe" %% "circe-parser" % V.circeVersion

  private lazy val circeJsonSchema = ("net.reactivecore" %% "circe-json-schema" % "0.3.0")
    .cross(CrossVersion.for3Use2_13)
    .exclude("io.circe", "circe-core_2.13")
    .exclude("io.circe", "circe-generic_2.13")
    .exclude("io.circe", "circe-parser_2.13")

  private lazy val jwtCirce = "com.github.jwt-scala" %% "jwt-circe" % V.jwtCirceVersion

  private lazy val zio = "dev.zio" %% "zio" % V.zio
  private lazy val zioPrelude = "dev.zio" %% "zio-prelude" % V.zioPreludeVersion

  private lazy val nimbusJoseJwt = "com.nimbusds" % "nimbus-jose-jwt" % "10.0.0-preview"

  private lazy val zioTest = "dev.zio" %% "zio-test" % V.zio % Test
  private lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio % Test
  private lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % V.zio % Test

  private lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.9" % Test

  // Dependency Modules
  private lazy val zioDependencies: Seq[ModuleID] = Seq(zio, zioPrelude, zioTest, zioTestSbt, zioTestMagnolia)
  private lazy val circeDependencies: Seq[ModuleID] = Seq(coreJwtCirce, genericJwtCirce, parserJwtCirce)
  private lazy val baseDependencies: Seq[ModuleID] =
    circeDependencies ++ zioDependencies :+ jwtCirce :+ circeJsonSchema :+ nimbusJoseJwt :+ scalaTest

  // Project Dependencies
  lazy val polluxVcJwtDependencies: Seq[ModuleID] = baseDependencies
}

lazy val D_PrismAgent = new {

  private lazy val zio = "dev.zio" %% "zio" % V.zio
  private lazy val zioConfig = "dev.zio" %% "zio-config" % V.zioConfig
  private lazy val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % V.zioConfig
  private lazy val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % V.zioConfig
  private lazy val zioJson = "dev.zio" %% "zio-json" % V.zioJson
  private lazy val zioInteropCats = "dev.zio" %% "zio-interop-cats" % V.zioCatsInterop

  private lazy val zioTest = "dev.zio" %% "zio-test" % V.zio % Test
  private lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio % Test
  private lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % V.zio % Test

  private lazy val zioHttp = "io.d11" %% "zhttp" % V.zioHttp

  private lazy val zioMetrics = "dev.zio" %% "zio-metrics-connectors" % V.zioMetrics

  private lazy val akkaTyped = "com.typesafe.akka" %% "akka-actor-typed" % V.akka
  private lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % V.akka
  private lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % V.akkaHttp
  private lazy val akkaSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % V.akkaHttp

  // private lazy val castorCore = "io.iohk.atala" %% "castor-core" % V.castor
  // private lazy val castorSqlDoobie = "io.iohk.atala" %% "castor-sql-doobie" % V.castor

  // private lazy val polluxCore = "io.iohk.atala" %% "pollux-core" % V.pollux
  // private lazy val polluxSqlDoobie = "io.iohk.atala" %% "pollux-sql-doobie" % V.pollux

  // private lazy val connectCore = "io.iohk.atala" %% "connect-core" % V.connect
  // private lazy val connectSqlDoobie = "io.iohk.atala" %% "connect-sql-doobie" % V.connect

  // private lazy val mercuryAgent = "io.iohk.atala" %% "mercury-agent-didcommx" % V.mercury
  // private lazy val mercuryPresentProof = "io.iohk.atala" %% "mercury-protocol-present-proof" % V.mercury

  // Added here to make prism-crypto works.
  // Once migrated to apollo, re-evaluate if this should be removed.
  private lazy val bouncyBcpkix = "org.bouncycastle" % "bcpkix-jdk15on" % V.bouncyCastle
  private lazy val bouncyBcprov = "org.bouncycastle" % "bcprov-jdk15on" % V.bouncyCastle

  private lazy val logback = "ch.qos.logback" % "logback-classic" % V.logback

  private lazy val tapirSwaggerUiBundle = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % V.tapir
  private lazy val tapirJsonZio = "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % V.tapir

  private lazy val tapirZioHttpServer = "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % V.tapir
  private lazy val tapirHttp4sServerZio = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % V.tapir
  private lazy val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % "0.23.12"

  private lazy val tapirRedocBundle = "com.softwaremill.sttp.tapir" %% "tapir-redoc-bundle" % V.tapir

  private lazy val tapirSttpStubServer =
    "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % V.tapir % Test
  private lazy val sttpClient3ZioJson = "com.softwaremill.sttp.client3" %% "zio-json" % "3.8.3" % Test

  private lazy val quillDoobie =
    "io.getquill" %% "quill-doobie" % V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")
  private lazy val postgresql = "org.postgresql" % "postgresql" % "42.2.8"
  private lazy val quillJdbcZio =
    "io.getquill" %% "quill-jdbc-zio" % V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")
  private lazy val flyway = "org.flywaydb" % "flyway-core" % V.flyway
  private lazy val testcontainers_scala_postgresql =
    "com.dimafeng" %% "testcontainers-scala-postgresql" % V.testContainersScalaPostgresql % Test

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
    zioHttp,
    zioMetrics,
  )
  // private lazy val castorDependencies: Seq[ModuleID] = Seq(castorCore, castorSqlDoobie)
  // private lazy val polluxDependencies: Seq[ModuleID] = Seq(polluxCore, polluxSqlDoobie)
  // private lazy val mercuryDependencies: Seq[ModuleID] = Seq(mercuryAgent)
  // private lazy val connectDependencies: Seq[ModuleID] = Seq(connectCore, connectSqlDoobie)
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

  private lazy val postgresDependencies: Seq[ModuleID] =
    Seq(quillDoobie, quillJdbcZio, postgresql, flyway, testcontainers_scala_postgresql)

  // Project Dependencies
  lazy val keyManagementDependencies: Seq[ModuleID] =
    baseDependencies ++
      // castorDependencies ++
      // mercuryDependencies ++
      bouncyDependencies

  lazy val serverDependencies: Seq[ModuleID] =
    baseDependencies ++
      akkaHttpDependencies ++
      // castorDependencies ++
      // polluxDependencies ++
      // mercuryDependencies ++
      // connectDependencies ++
      tapirDependencies ++
      postgresDependencies
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
  .settings(libraryDependencies += "com.google.zxing" % "core" % "3.5.0")
  .settings(libraryDependencies += D.zioHttp)
  .dependsOn(agentDidcommx)

// ///** TODO Demos agents and services implementation with did-scala */
// lazy val agentDidScala =
//   project
//     .in(file("mercury/mercury-library/agent-did-scala"))
//     .settings(name := "mercury-agent-didscala")
//     .settings(skip / publish := true)
//     .dependsOn(agent)

// ### Test coverage ###
sys.env
  .get("SBT_SCOVERAGE")
  .map { _ =>
    lazy val coverageDataDir: SettingKey[File] =
      settingKey[File]("directory where the measurements and report files will be stored")
    coverageDataDir := target.value / "coverage"
  }
  .toSeq

// ### ReleaseStep ###
sys.env
  .get("SBT_PACKAGER") // SEE also plugin.sbt
  .map { _ =>
    println("### Config SBT_PACKAGER (releaseProcess) ###")
  import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    publishArtifacts,
    setNextVersion
  )
  }
  .toSeq

// #####################
// #####  castor  ######
// #####################

val castorCommonSettings = Seq(
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
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

lazy val castorDoobie = project
  .in(file("castor/lib/sql-doobie"))
  .settings(castorCommonSettings)
  .settings(
    name := "castor-sql-doobie",
    libraryDependencies ++= D_Castor.sqlDoobieDependencies
  )
  .dependsOn(shared, castorCore)

// #####################
// #####  pollux  ######
// #####################

val polluxCommonSettings = Seq(
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
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
  .dependsOn(protocolIssueCredential, protocolPresentProof, resolver)

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
import sbtghpackages.GitHubPackagesPlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

def prismAgentConnectCommonSettings = polluxCommonSettings

lazy val prismAgentWalletAPI = project
  .in(file("prism-agent/service/wallet-api"))
  .settings(prismAgentConnectCommonSettings)
  .settings(
    name := "prism-agent-wallet-api",
    libraryDependencies ++= PrismAgentDependencies.keyManagementDependencies
  )
  .dependsOn(agentDidcommx)
  .dependsOn(castorCore, castorDoobie)

lazy val prismAgentServer = project
  .in(file("prism-agent/service/server"))
  .settings(prismAgentConnectCommonSettings)
  .settings(
    name := "prism-agent",
    fork := true,
    libraryDependencies ++= PrismAgentDependencies.serverDependencies,
    Compile / mainClass := Some("io.iohk.atala.agent.server.Main"),
    // OpenAPI settings
    Compile / unmanagedResourceDirectories += baseDirectory.value / ".." / "api",
    Compile / sourceGenerators += openApiGenerateClasses,
    openApiGeneratorSpec := baseDirectory.value / ".." / "api" / "http/prism-agent-openapi-spec.yaml",
    openApiGeneratorConfig := baseDirectory.value / "openapi/generator-config/config2.yaml",
    openApiGeneratorImportMapping := Seq(
      "DidOperationType",
      "DidOperationStatus"
    )
      .map(model => (model, s"io.iohk.atala.agent.server.http.model.OASModelPatches.$model"))
      .toMap,
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerUsername := Some("input-output-hk"),
    // Docker / githubOwner := "atala-prism-building-blocks", // not used by any other settings
    Docker / dockerRepository := Some("ghcr.io"),
    dockerExposedPorts := Seq(8080, 8085, 8090),
    dockerBaseImage := "openjdk:11",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.iohk.atala.agent.server.buildinfo"
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .enablePlugins(OpenApiGeneratorPlugin, BuildInfoPlugin)
  .dependsOn(prismAgentWalletAPI)
  .dependsOn(
    agent,
    polluxCore,
    polluxDoobie,
    connectCore,
    connectDoobie,
    castorCore,
    castorDoobie
  )
