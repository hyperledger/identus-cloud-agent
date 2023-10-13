plugins {
    idea
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("net.serenity-bdd.serenity-gradle-plugin") version "4.0.14"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.0"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/input-output-hk/atala-automation/")
        credentials {
            username = System.getenv("ATALA_GITHUB_ACTOR")
            password = System.getenv("ATALA_GITHUB_TOKEN")
        }
    }
    maven {
        url = uri("https://maven.pkg.github.com/hyperledger-labs/open-enterprise-agent/")
        credentials {
            username = System.getenv("ATALA_GITHUB_ACTOR")
            password = System.getenv("ATALA_GITHUB_TOKEN")
        }
    }
}

dependencies {
    // Logging
    implementation("org.slf4j:slf4j-log4j12:2.0.5")
    // Beautify async waits
    implementation("org.awaitility:awaitility-kotlin:4.2.0")
    // Test engines and reports
    testImplementation("junit:junit:4.13.2")
    implementation("net.serenity-bdd:serenity-core:4.0.14")
    implementation("net.serenity-bdd:serenity-cucumber:4.0.14")
    implementation("net.serenity-bdd:serenity-screenplay-rest:4.0.14")
    testImplementation("net.serenity-bdd:serenity-ensure:4.0.14")
    // HTTP listener
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-client-apache:2.3.0")
    // RestAPI client
    implementation("io.iohk.atala.prism:prism-kotlin-client:1.15.0")
    // Test helpers library
    testImplementation("io.iohk.atala:atala-automation:0.3.0")
    // Hoplite for configuration
    implementation("com.sksamuel.hoplite:hoplite-core:2.7.5")
    implementation("com.sksamuel.hoplite:hoplite-hocon:2.7.5")
}

buildscript {
    dependencies {
        classpath("net.serenity-bdd:serenity-single-page-report:4.0.14")
        classpath("net.serenity-bdd:serenity-json-summary-report:4.0.14")
    }
}

/**
 * Add HTML one-pager and JSON summary report to be produced
 */
serenity {
    reports = listOf("single-page-html", "json-summary")
}

tasks.test {
    testLogging.showStandardStreams = true
    systemProperty("cucumber.filter.tags", System.getProperty("cucumber.filter.tags"))
}

kotlin {
    jvmToolchain(19)
}

ktlint {
    disabledRules.set(setOf("no-wildcard-imports"))
}
