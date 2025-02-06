import org.scoverage.coveralls.Imports.CoverallsKeys.*
import sbtbuildinfo.BuildInfoPlugin.autoImport.*

// externalResolvers += "ScalaLibrary packages" at "https://maven.pkg.github.com/input-output-hk/anoncreds-rs" // use plugin"sbt-github-packages"

inThisBuild(
  Seq(
    organization := "org.hyperledger",
    scalaVersion := "3.3.4",
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    fork := true,
    run / connectInput := true,
    releaseUseGlobalVersion := false,
    versionScheme := Some("semver-spec"),
    resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    resolvers += "jitpack" at "https://jitpack.io",
  )
)

// Fixes a bug with concurrent packages download from GitHub registry
Global / concurrentRestrictions += Tags.limit(Tags.Network, 1)

coverageDataDir := target.value / "coverage"
coberturaFile := target.value / "coverage" / "coverage-report" / "cobertura.xml"

inThisBuild(
  Seq(
    scalacOptions ++= Seq("-encoding", "UTF-8"),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
    ),
    scalacOptions += "-Wunused:all",
    scalacOptions += "-Wconf:cat=deprecation:warning,any:error", // "-Wconf:help",
    // scalacOptions += "-Yexplicit-nulls",
    // scalacOptions += "-Ysafe-init",
    // scalacOptions +=  "-Werror", // <=> "-Xfatal-warnings"
    scalacOptions += "-Dquill.macro.log=false", // disable quill macro logs // TODO https://github.com/zio/zio-protoquill/issues/470,
    scalacOptions ++= Seq("-Xmax-inlines", "50") // increase above 32 (https://github.com/circe/circe/issues/2162)
  )
)

lazy val V = new {
  val munit = "1.0.2" // "0.7.29"
  val munitZio = "0.3.0"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.1.11"
  val zioConfig = "4.0.2"
  val zioLogging = "2.3.1"
  val zioJson = "0.7.3"
  val zioHttp = "3.0.1"
  val zioCatsInterop = "3.3.0" // TODO "23.1.0.2" // https://mvnrepository.com/artifact/dev.zio/zio-interop-cats
  val zioMetricsConnector = "2.3.1"
  val zioMock = "1.0.0-RC12"
  val zioKafka = "2.8.2"
  val mockito = "3.2.18.0"
  val monocle = "3.3.0"

  val tapir = "1.11.7" // scala-steward:off // TODO "1.10.5"
  val http4sBlaze = "0.23.15" // scala-steward:off  // TODO "0.23.16"

  val typesafeConfig = "1.4.3"
  val protobuf = "3.1.9"
  val grpcOkHttp = "1.63.0"

  val testContainersScala = "0.41.8"
  val testContainersJavaKeycloak = "3.2.0" // scala-steward:off

  val doobie = "1.0.0-RC5"
  val quill = "4.8.5"
  val flyway = "11.2.0"
  val postgresDriver = "42.7.4"
  val logback = "1.5.16"
  val slf4j = "2.0.16"

  val scalaUri = "4.0.3"

  val jwtZioVersion = "10.0.1"
  val zioPreludeVersion = "1.0.0-RC31"

  val apollo = "1.3.5"

  val jsonSchemaValidator = "1.3.2" // scala-steward:off //TODO 1.3.2 need to fix:
  // [error] 	org.hyperledger.identus.pollux.core.model.schema.AnoncredSchemaTypeSpec
  // [error] 	org.hyperledger.identus.pollux.core.model.schema.CredentialSchemaSpec

  val vaultDriver = "6.2.0"
  val micrometer = "1.13.10"

  val nimbusJwt = "9.48"
  val keycloak = "23.0.7" // scala-steward:off //TODO 24.0.3 // update all quay.io/keycloak/keycloak

}

/** Dependencies */
lazy val D = new {
  val zio: ModuleID = "dev.zio" %% "zio" % V.zio
  val zioStreams: ModuleID = "dev.zio" %% "zio-streams" % V.zio
  val zioLog: ModuleID = "dev.zio" %% "zio-logging" % V.zioLogging
  val zioSLF4J: ModuleID = "dev.zio" %% "zio-logging-slf4j" % V.zioLogging
  val zioJson: ModuleID = "dev.zio" %% "zio-json" % V.zioJson
  val zioConcurrent: ModuleID = "dev.zio" %% "zio-concurrent" % V.zio
  val zioHttp: ModuleID = "dev.zio" %% "zio-http" % V.zioHttp
  val zioKafka: ModuleID = "dev.zio" %% "zio-kafka" % V.zioKafka excludeAll (
    ExclusionRule("dev.zio", "zio_3"),
    ExclusionRule("dev.zio", "zio-streams_3")
  )
  val zioCatsInterop: ModuleID = "dev.zio" %% "zio-interop-cats" % V.zioCatsInterop
  val zioMetricsConnectorMicrometer: ModuleID = "dev.zio" %% "zio-metrics-connectors-micrometer" % V.zioMetricsConnector
  val tapirPrometheusMetrics: ModuleID = "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % V.tapir
  val micrometer: ModuleID = "io.micrometer" % "micrometer-registry-prometheus" % V.micrometer
  val micrometerPrometheusRegistry = "io.micrometer" % "micrometer-core" % V.micrometer
  val scalaUri = Seq(
    "io.lemonlabs" %% "scala-uri" % V.scalaUri exclude (
      "org.typelevel",
      "cats-parse_3"
    ), // Exclude cats-parse to avoid deps conflict
    "org.typelevel" % "cats-parse_3" % "1.1.0", // Replace with version 1.0.0
  )

  val zioConfig: ModuleID = "dev.zio" %% "zio-config" % V.zioConfig
  val zioConfigMagnolia: ModuleID = "dev.zio" %% "zio-config-magnolia" % V.zioConfig
  val zioConfigTypesafe: ModuleID = "dev.zio" %% "zio-config-typesafe" % V.zioConfig

  val networkntJsonSchemaValidator = "com.networknt" % "json-schema-validator" % V.jsonSchemaValidator
  val jwtZio = "com.github.jwt-scala" %% "jwt-zio-json" % V.jwtZioVersion
  val jsonCanonicalization: ModuleID = "io.github.erdtman" % "java-json-canonicalization" % "1.1"
  val titaniumJsonLd: ModuleID = "com.apicatalog" % "titanium-json-ld" % "1.4.1"
  val jakartaJson: ModuleID = "org.glassfish" % "jakarta.json" % "2.0.1"
  val ironVC: ModuleID = "com.apicatalog" % "iron-verifiable-credentials" % "0.14.0"
  val scodecBits: ModuleID = "org.scodec" %% "scodec-bits" % "1.2.1"
  val jaywayJsonPath: ModuleID = "com.jayway.jsonpath" % "json-path" % "2.9.0"

  // https://mvnrepository.com/artifact/org.didcommx/didcomm/0.3.2
  val didcommx: ModuleID = "org.didcommx" % "didcomm" % "0.3.2"
  val peerDidcommx: ModuleID = "org.didcommx" % "peerdid" % "0.5.0"
  val didScala: ModuleID = "app.fmgp" %% "did" % "0.0.0+113-61efa271-SNAPSHOT"

  val nimbusJwt: ModuleID = "com.nimbusds" % "nimbus-jose-jwt" % V.nimbusJwt

  val typesafeConfig: ModuleID = "com.typesafe" % "config" % V.typesafeConfig
  val scalaPbRuntime: ModuleID =
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  val scalaPbGrpc: ModuleID = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
  val grpcOkHttp: ModuleID = "io.grpc" % "grpc-okhttp" % V.grpcOkHttp

  val testcontainersPostgres: ModuleID =
    "com.dimafeng" %% "testcontainers-scala-postgresql" % V.testContainersScala % Test
  val testcontainersVault: ModuleID = "com.dimafeng" %% "testcontainers-scala-vault" % V.testContainersScala % Test
  val testcontainersKeycloak: ModuleID =
    "com.github.dasniko" % "testcontainers-keycloak" % V.testContainersJavaKeycloak % Test

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
  val zioMock: ModuleID = "dev.zio" %% "zio-mock" % V.zioMock
  val zioPrelude: ModuleID = "dev.zio" %% "zio-prelude" % V.zioPreludeVersion
  val mockito: ModuleID = "org.scalatestplus" %% "mockito-4-11" % V.mockito % Test
  val monocle: ModuleID = "dev.optics" %% "monocle-core" % V.monocle % Test
  val monocleMacro: ModuleID = "dev.optics" %% "monocle-macro" % V.monocle % Test
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19" % Test

  val apollo = Seq( // TODO remove exclude after fix https://github.com/hyperledger/identus-apollo/issues/192
    "io.iohk.atala.prism.apollo" % "apollo-jvm" % V.apollo exclude (
      "net.jcip",
      "jcip-annotations"
    ), // Exclude because of license
    "com.github.stephenc.jcip" % "jcip-annotations" % "1.0-1" % Runtime, // Replace for net.jcip % jcip-annotations"
  )

  // LIST of Dependencies
  val doobieDependencies: Seq[ModuleID] =
    Seq(doobiePostgres, doobieHikari, flyway)
}

lazy val D_Shared = new {
  lazy val dependencies: Seq[ModuleID] =
    Seq(
      D.typesafeConfig,
      D.scalaPbGrpc,
      D.zio,
      D.zioConcurrent,
      D.zioHttp,
      D.zioKafka,
      D.zioPrelude,
      // FIXME: split shared DB stuff as subproject?
      D.doobieHikari,
      D.doobiePostgres,
      D.zioCatsInterop,
    ) ++ D.scalaUri
}

lazy val D_SharedJson = new {
  lazy val dependencies: Seq[ModuleID] =
    Seq(
      D.zio,
      D.zioJson,
      D.jsonCanonicalization,
      D.titaniumJsonLd,
      D.jakartaJson,
      D.ironVC,
      D.scodecBits,
      D.networkntJsonSchemaValidator,
      D.jaywayJsonPath
    )
}

lazy val D_SharedCrypto = new {
  lazy val dependencies: Seq[ModuleID] =
    Seq(
      D.zioJson,
      D.nimbusJwt,
      D.zioTest,
      D.zioTestSbt,
      D.zioTestMagnolia,
    ) ++ D.apollo
}

lazy val D_SharedTest = new {
  lazy val dependencies: Seq[ModuleID] =
    D_Shared.dependencies ++ Seq(
      D.testcontainersPostgres,
      D.testcontainersVault,
      D.testcontainersKeycloak,
      D.zioCatsInterop,
      D.zioJson,
      D.zioHttp,
      D.zioTest,
      D.zioTestSbt,
      D.zioTestMagnolia,
      D.zioMock
    )
}

lazy val D_Connect = new {

  private lazy val logback = "ch.qos.logback" % "logback-classic" % V.logback % Test

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] =
    Seq(D.zio, D.zioTest, D.zioTestSbt, D.zioTestMagnolia, D.zioMock, D.testcontainersPostgres, logback)

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] =
    baseDependencies
  lazy val sqlDoobieDependencies: Seq[ModuleID] =
    baseDependencies ++ D.doobieDependencies ++ Seq(D.zioCatsInterop)
}

lazy val D_Castor = new {
  // Dependency Modules
  val baseDependencies: Seq[ModuleID] =
    Seq(
      D.zio,
      D.zioTest,
      D.zioMock,
      D.zioTestSbt,
      D.zioTestMagnolia,
    )

  // Project Dependencies
  val coreDependencies: Seq[ModuleID] = baseDependencies
}

lazy val D_Pollux = new {
  val logback = "ch.qos.logback" % "logback-classic" % V.logback % Test
  val slf4jApi = "org.slf4j" % "slf4j-api" % V.slf4j % Test
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % V.slf4j % Test

  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % V.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % V.doobie

  val flyway = "org.flywaydb" % "flyway-core" % V.flyway

  val quillJdbcZio = "io.getquill" %% "quill-jdbc-zio" %
    V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")

  val quillDoobie = "io.getquill" %% "quill-doobie" %
    V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")

  // Dependency Modules
  val baseDependencies: Seq[ModuleID] = Seq(
    D.zio,
    D.zioJson,
    D.zioHttp,
    D.zioTest,
    D.zioTestSbt,
    D.zioTestMagnolia,
    D.zioMock,
    D.munit,
    D.munitZio,
    // shared,
    logback,
    slf4jApi,
    slf4jSimple
  )

  val doobieDependencies: Seq[ModuleID] = Seq(
    D.zioCatsInterop,
    D.doobiePostgres,
    D.doobieHikari,
    D.testcontainersPostgres,
    flyway,
    quillDoobie,
    quillJdbcZio,
  )

  // Project Dependencies
  val coreDependencies: Seq[ModuleID] = baseDependencies
  val sqlDoobieDependencies: Seq[ModuleID] = baseDependencies ++ doobieDependencies
}

lazy val D_Pollux_VC_JWT = new {

  val zio = "dev.zio" %% "zio" % V.zio
  val zioPrelude = "dev.zio" %% "zio-prelude" % V.zioPreludeVersion

  val zioTest = "dev.zio" %% "zio-test" % V.zio % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio % Test
  val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % V.zio % Test

  // Dependency Modules
  val zioDependencies: Seq[ModuleID] = Seq(zio, zioPrelude, zioTest, zioTestSbt, zioTestMagnolia)
  val baseDependencies: Seq[ModuleID] =
    zioDependencies :+ D.jwtZio :+ D.networkntJsonSchemaValidator :+ D.nimbusJwt :+ D.scalaTest

  // Project Dependencies
  lazy val polluxVcJwtDependencies: Seq[ModuleID] = baseDependencies
}

lazy val D_EventNotification = new {
  val zio = "dev.zio" %% "zio" % V.zio
  val zioTest = "dev.zio" %% "zio-test" % V.zio % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio % Test
  val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % V.zio % Test

  val zioDependencies: Seq[ModuleID] = Seq(zio, zioTest, zioTestSbt, zioTestMagnolia)
  val baseDependencies: Seq[ModuleID] = zioDependencies
}

lazy val D_Pollux_AnonCreds = new {
  val baseDependencies: Seq[ModuleID] = Seq(D.zio, D.zioJson)
}

lazy val D_CloudAgent = new {
  val logback = "ch.qos.logback" % "logback-classic" % V.logback

  val tapirSwaggerUiBundle = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % V.tapir
  val tapirJsonZio = "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % V.tapir

  val tapirZioHttpServer = "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % V.tapir
  val tapirHttp4sServerZio = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % V.tapir
  val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % V.http4sBlaze

  val tapirRedocBundle = "com.softwaremill.sttp.tapir" %% "tapir-redoc-bundle" % V.tapir

  val tapirSttpStubServer =
    "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % V.tapir % Test
  val sttpClient3ZioJson = "com.softwaremill.sttp.client3" %% "zio-json" % "3.10.2" % Test

  val quillDoobie =
    "io.getquill" %% "quill-doobie" % V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")
  val postgresql = "org.postgresql" % "postgresql" % V.postgresDriver
  val quillJdbcZio =
    "io.getquill" %% "quill-jdbc-zio" % V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")

  val flyway = "org.flywaydb" % "flyway-core" % V.flyway

  val vaultDriver = "io.github.jopenlibs" % "vault-java-driver" % V.vaultDriver
  val keycloakAuthz = "org.keycloak" % "keycloak-authz-client" % V.keycloak

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
    D.zioMetricsConnectorMicrometer,
    D.tapirPrometheusMetrics,
    D.micrometer,
    D.micrometerPrometheusRegistry
  )
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
    Seq(quillDoobie, quillJdbcZio, postgresql, flyway, D.testcontainersPostgres, D.zioCatsInterop)

  // Project Dependencies
  lazy val keyManagementDependencies: Seq[ModuleID] =
    baseDependencies ++ D.doobieDependencies ++ Seq(D.zioCatsInterop, D.zioMock, vaultDriver)

  lazy val iamDependencies: Seq[ModuleID] = Seq(keycloakAuthz, D.jwtZio)

  lazy val serverDependencies: Seq[ModuleID] =
    baseDependencies ++ tapirDependencies ++ postgresDependencies ++ Seq(
      D.zioMock,
      D.mockito,
      D.monocle,
      D.monocleMacro
    )
}

publish / skip := true

val commonSetttings = Seq(
  testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  libraryDependencies ++= Seq(D.zioTest, D.zioTestSbt, D.zioTestMagnolia),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
  resolvers += "jitpack" at "https://jitpack.io",
  // Override 'updateLicenses' for all project to inject custom DependencyResolution.
  // https://github.com/sbt/sbt-license-report/blob/9675cedb19c794de1119cbcf46a255fc8dcd5d4e/src/main/scala/sbtlicensereport/SbtLicenseReport.scala#L84
  updateLicenses := {
    import sbt.librarymanagement.DependencyResolution
    import sbt.librarymanagement.ivy.IvyDependencyResolution
    import sbtlicensereport.license

    val ignore = update.value
    val overrides = licenseOverrides.value.lift
    val depExclusions = licenseDepExclusions.value.lift
    val originatingModule = DepModuleInfo(organization.value, name.value, version.value)
    val resolution =
      DependencyResolution(new LicenseReportCustomDependencyResolution(ivyConfiguration.value, ivyModule.value))
    license.LicenseReport.makeReport(
      ivyModule.value,
      resolution,
      licenseConfigurations.value,
      licenseSelection.value,
      overrides,
      depExclusions,
      originatingModule,
      streams.value.log
    )
  }
)

lazy val commonConfigure: Project => Project = _.settings(
  Compile / scalacOptions += "-Yimports:java.lang,scala,scala.Predef,org.hyperledger.identus.Predef",
  Test / scalacOptions -= "-Yimports:java.lang,scala,scala.Predef,org.hyperledger.identus.Predef",
).dependsOn(predef)

// #####################
// #####  shared  ######
// #####################

lazy val predef = (project in file("shared/predef"))

lazy val shared = (project in file("shared/core"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "shared",
    crossPaths := false,
    libraryDependencies ++= D_Shared.dependencies
  )

lazy val sharedJson = (project in file("shared/json"))
  .settings(commonSetttings)
  .settings(
    name := "shared-json",
    crossPaths := false,
    libraryDependencies ++= D_SharedJson.dependencies
  )
  .dependsOn(shared)

lazy val sharedCrypto = (project in file("shared/crypto"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "shared-crypto",
    crossPaths := false,
    libraryDependencies ++= D_SharedCrypto.dependencies
  )
  .dependsOn(shared)

lazy val sharedTest = (project in file("shared/test"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "shared-test",
    crossPaths := false,
    libraryDependencies ++= D_SharedTest.dependencies
  )
  .dependsOn(shared)

// #########################
// ### Models & Services ###
// #########################

/** Just data models and interfaces of service.
  *
  * This module must not depend on external libraries!
  */
lazy val models = project
  .in(file("mercury/models"))
  .configure(commonConfigure)
  .settings(name := "mercury-data-models")
  .settings(
    libraryDependencies ++= Seq(D.zio)
  )
  .settings(libraryDependencies += D.nimbusJwt) // FIXME just for the DidAgent
  .dependsOn(shared)

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
  .in(file("mercury/protocol-connection"))
  .configure(commonConfigure)
  .settings(name := "mercury-protocol-connection")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models, protocolInvitation)

lazy val protocolCoordinateMediation = project
  .in(file("mercury/protocol-coordinate-mediation"))
  .configure(commonConfigure)
  .settings(name := "mercury-protocol-coordinate-mediation")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolDidExchange = project
  .in(file("mercury/protocol-did-exchange"))
  .configure(commonConfigure)
  .settings(name := "mercury-protocol-did-exchange")
  .settings(libraryDependencies += D.zio)
  .dependsOn(models, protocolInvitation)

lazy val protocolInvitation = project
  .in(file("mercury/protocol-invitation"))
  .configure(commonConfigure)
  .settings(name := "mercury-protocol-invitation")
  .settings(libraryDependencies += D.zio)
  .settings(
    libraryDependencies ++= Seq(
      D.munit,
      D.munitZio
    )
  )
  .dependsOn(models)

// lazy val protocolMercuryMailbox = project
//   .in(file("mercury/protocol-mercury-mailbox"))
//   .settings(predefSetttings)
//   .settings(name := "mercury-protocol-mailbox")
//   .settings(libraryDependencies += D.zio)
//   .dependsOn(models, protocolInvitation, protocolRouting)

lazy val protocolLogin = project
  .in(file("mercury/protocol-outofband-login"))
  .configure(commonConfigure)
  .settings(name := "mercury-protocol-outofband-login")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolReportProblem = project
  .in(file("mercury/protocol-report-problem"))
  .configure(commonConfigure)
  .settings(name := "mercury-protocol-report-problem")
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolRouting = project
  .in(file("mercury/protocol-routing"))
  .configure(commonConfigure)
  .settings(name := "mercury-protocol-routing-2-0")
  .settings(libraryDependencies += D.zio)
  .dependsOn(models)

lazy val protocolIssueCredential = project
  .in(file("mercury/protocol-issue-credential"))
  .configure(commonConfigure)
  .settings(name := "mercury-protocol-issue-credential")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models, protocolInvitation)

lazy val protocolRevocationNotification = project
  .in(file("mercury/protocol-revocation-notification"))
  .configure(commonConfigure)
  .settings(name := "mercury-protocol-revocation-notification")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolPresentProof = project
  .in(file("mercury/protocol-present-proof"))
  .configure(commonConfigure)
  .settings(name := "mercury-protocol-present-proof")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models, protocolInvitation)

lazy val vc = project
  .in(file("mercury/vc"))
  .configure(commonConfigure)
  .settings(name := "mercury-verifiable-credentials")
  .dependsOn(protocolIssueCredential, protocolPresentProof) //TODO merge those two modules into this one

lazy val protocolTrustPing = project
  .in(file("mercury/protocol-trust-ping"))
  .configure(commonConfigure)
  .settings(name := "mercury-protocol-trust-ping")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

// ################
// ### Resolver ###
// ################

// TODO move stuff to the models module
lazy val resolver = project // maybe merge into models
  .in(file("mercury/resolver"))
  .configure(commonConfigure)
  .settings(name := "mercury-resolver")
  .settings(
    libraryDependencies ++= Seq(
      D.didcommx,
      D.peerDidcommx,
      D.munit,
      D.munitZio,
      D.nimbusJwt,
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(models)

// ##############
// ### Agents ###
// ##############

lazy val agent = project // maybe merge into models
  .in(file("mercury/agent"))
  .configure(commonConfigure)
  .settings(name := "mercury-agent-core")
  .settings(libraryDependencies ++= Seq(D.zioLog, D.zioSLF4J))
  .dependsOn(
    models,
    resolver,
    protocolCoordinateMediation,
    protocolInvitation,
    protocolRouting,
    // protocolMercuryMailbox,
    protocolLogin,
    protocolIssueCredential,
    protocolRevocationNotification,
    protocolPresentProof,
    vc,
    protocolConnection,
    protocolReportProblem,
    protocolTrustPing,
  )

/** agents implementation with didcommx */
lazy val agentDidcommx = project
  .in(file("mercury/agent-didcommx"))
  .configure(commonConfigure)
  .settings(name := "mercury-agent-didcommx")
  .settings(libraryDependencies += D.didcommx)
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(agent) //modelsDidcommx

// ///** TODO Demos agents and services implementation with did-scala */
// lazy val agentDidScala =
//   project
//     .in(file("mercury/agent-did-scala"))
//     .settings(name := "mercury-agent-didscala")
//     .settings(skip / publish := true)
//     .dependsOn(agent)

// ####################
// ###  Prism Node ####
// ####################
val prismNodeClient = project
  .in(file("prism-node/client/scala-client"))
  .configure(commonConfigure)
  .settings(
    name := "prism-node-client",
    libraryDependencies ++= Seq(D.scalaPbGrpc, D.scalaPbRuntime, D.grpcOkHttp),
    coverageEnabled := false,
    // gRPC settings
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    Compile / PB.protoSources := Seq(
      baseDirectory.value / "api" / "grpc",
      (Compile / resourceDirectory).value // includes scalapb codegen package wide config
    )
  )

// #####################
// #####  castor  ######
// #####################

lazy val castorCore = project
  .in(file("castor"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "castor-core",
    libraryDependencies ++= D_Castor.coreDependencies
  )
  .dependsOn(shared, prismNodeClient)
  .dependsOn(sharedCrypto % "compile->compile;test->test")

// #####################
// #####  pollux  ######
// #####################

lazy val polluxVcJWT = project
  .in(file("pollux/vc-jwt"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "pollux-vc-jwt",
    libraryDependencies ++= D_Pollux_VC_JWT.polluxVcJwtDependencies
  )
  .dependsOn(castorCore, sharedJson)

lazy val polluxCore = project
  .in(file("pollux/core"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "pollux-core",
    libraryDependencies ++= D_Pollux.coreDependencies
  )
  .dependsOn(
    shared,
    castorCore % "compile->compile;test->test", // Test is for MockDIDService
    agentWalletAPI % "compile->compile;test->test", // Test is for MockManagedDIDService
    vc,
    resolver,
    agentDidcommx,
    eventNotification,
    polluxAnoncreds,
    polluxVcJWT,
    polluxSDJWT,
    polluxPreX % "compile->compile;test->test", // Test is for example resources
  )

lazy val polluxDoobie = project
  .in(file("pollux/sql-doobie"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "pollux-sql-doobie",
    libraryDependencies ++= D_Pollux.sqlDoobieDependencies
  )
  .dependsOn(polluxCore % "compile->compile;test->test")
  .dependsOn(shared)
  .dependsOn(sharedTest % "test->test")

lazy val polluxPreX = project
  .in(file("pollux/prex"))
  .settings(commonSetttings)
  .settings(name := "pollux-prex")
  .dependsOn(shared, sharedJson, polluxVcJWT)

// ########################
// ### Pollux Anoncreds ###
// ########################

lazy val polluxAnoncreds = project
  .in(file("pollux/anoncreds"))
  .configure(commonConfigure)
  .settings(
    name := "pollux-anoncreds",
    Compile / unmanagedJars += baseDirectory.value / "anoncreds-jvm-1.0-SNAPSHOT.jar",
    Compile / unmanagedResourceDirectories ++= Seq(
      baseDirectory.value / "native-lib" / "NATIVE"
    ),
    libraryDependencies ++= D_Pollux_AnonCreds.baseDependencies
  )

lazy val polluxAnoncredsTest = project
  .in(file("pollux/anoncredsTest"))
  .configure(commonConfigure)
  .settings(libraryDependencies += D.scalaTest)
  .dependsOn(polluxAnoncreds % "compile->test")

lazy val polluxSDJWT = project
  .in(file("pollux/sd-jwt"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "pollux-sd-jwt",
    libraryDependencies += "io.iohk.atala" % "sd-jwt-kmp-jvm" % "0.1.2"
  )
  .dependsOn(sharedCrypto)

// #####################
// #####  connect  #####
// #####################

lazy val connectCore = project
  .in(file("connect/core"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "connect-core",
    libraryDependencies ++= D_Connect.coreDependencies,
    Test / publishArtifact := true
  )
  .dependsOn(shared)
  .dependsOn(protocolConnection, protocolReportProblem, eventNotification)

lazy val connectDoobie = project
  .in(file("connect/sql-doobie"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "connect-sql-doobie",
    libraryDependencies ++= D_Connect.sqlDoobieDependencies
  )
  .dependsOn(shared)
  .dependsOn(sharedTest % "test->test")
  .dependsOn(connectCore % "compile->compile;test->test")

// ############################
// #### Event Notification ####
// ############################

lazy val eventNotification = project
  .in(file("event-notification"))
  .configure(commonConfigure)
  .settings(
    name := "event-notification",
    libraryDependencies ++= D_EventNotification.baseDependencies
  )
  .dependsOn(shared)

// #####################
// #### Cloud Agent ####
// #####################

lazy val agentWalletAPI = project
  .in(file("cloud-agent/service/wallet-api"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "cloud-agent-wallet-api",
    libraryDependencies ++=
      D_CloudAgent.keyManagementDependencies ++
        D_CloudAgent.iamDependencies ++
        D_CloudAgent.postgresDependencies ++
        Seq(D.zioMock)
  )
  .dependsOn(
    agentDidcommx,
    castorCore,
    eventNotification
  )
  .dependsOn(sharedTest % "test->test")
  .dependsOn(sharedCrypto % "compile->compile;test->test")

lazy val cloudAgentServer = project
  .in(file("cloud-agent/service/server"))
  .configure(commonConfigure)
  .settings(commonSetttings)
  .settings(
    name := "identus-cloud-agent",
    fork := true,
    libraryDependencies ++= D_CloudAgent.serverDependencies,
    excludeDependencies ++= Seq(
      // Exclude `protobuf-javalite` from all dependencies since we're using scalapbRuntime which already include `protobuf-java`
      // Having both may introduce conflict on some api https://github.com/protocolbuffers/protobuf/issues/8104
      ExclusionRule("com.google.protobuf", "protobuf-javalite")
    ),
    Compile / mainClass := Some("org.hyperledger.identus.agent.server.MainApp"),
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerUsername := Some("hyperledger"), // https://github.com/hyperledger
    Docker / dockerRepository := Some("ghcr.io"),
    dockerExposedPorts := Seq(8085, 8090),
    // Official docker image for openjdk 21 with curl and bash
    dockerBaseImage := "openjdk:21-jdk",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "org.hyperledger.identus.agent.server.buildinfo",
    Compile / packageDoc / publishArtifact := false
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(agentWalletAPI % "compile->compile;test->test")
  .dependsOn(
    sharedTest % "test->test",
    agent,
    polluxCore % "compile->compile;test->test",
    polluxDoobie,
    polluxAnoncreds,
    connectCore % "compile->compile;test->test", // Test is for MockConnectionService
    connectDoobie,
    castorCore,
    eventNotification,
  )

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
  ReleaseStep(releaseStepTask(cloudAgentServer / Docker / stage)),
  setNextVersion
)

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  shared,
  sharedJson,
  sharedCrypto,
  sharedTest,
  models,
  protocolConnection,
  protocolCoordinateMediation,
  protocolDidExchange,
  protocolInvitation,
  // protocolMercuryMailbox,
  protocolLogin,
  protocolReportProblem,
  protocolRouting,
  protocolIssueCredential,
  protocolRevocationNotification,
  protocolPresentProof,
  vc,
  protocolTrustPing,
  resolver,
  agent,
  agentDidcommx,
  castorCore,
  polluxVcJWT,
  polluxCore,
  polluxDoobie,
  polluxAnoncreds,
  polluxAnoncredsTest,
  polluxSDJWT,
  polluxPreX,
  connectCore,
  connectDoobie,
  agentWalletAPI,
  cloudAgentServer,
  eventNotification,
)

lazy val root = project
  .in(file("."))
  .aggregate(aggregatedProjects: _*)
