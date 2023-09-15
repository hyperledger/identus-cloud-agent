plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    idea
    jacoco
    id("net.serenity-bdd.serenity-gradle-plugin") version "4.0.1"
    kotlin("plugin.serialization") version "1.9.0"
}

repositories {
    mavenCentral()
}

dependencies {
    // Logging
    implementation("org.slf4j:slf4j-log4j12:2.0.5")
    // Beautify async waits
    implementation("org.awaitility:awaitility-kotlin:4.2.0")
    // Test engines and reports
    testImplementation("junit:junit:4.13.2")
    implementation("net.serenity-bdd:serenity-core:4.0.1")
    implementation("net.serenity-bdd:serenity-cucumber:4.0.1")
    implementation("net.serenity-bdd:serenity-screenplay-rest:4.0.1")
    testImplementation("net.serenity-bdd:serenity-ensure:4.0.1")
    // Beautify exceptions handling assertions
    testImplementation("org.assertj:assertj-core:3.23.1")
    // Navigate through Json with xpath
    testImplementation("com.jayway.jsonpath:json-path:2.7.0")
    // HTTP listener
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-client-apache:2.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}

buildscript {
    dependencies {
        classpath("net.serenity-bdd:serenity-single-page-report:4.0.1")
        classpath("net.serenity-bdd:serenity-json-summary-report:4.0.1")
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
