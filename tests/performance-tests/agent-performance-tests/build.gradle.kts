import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    id("io.gatling.gradle") version "3.9.0.2"
}

group = "io.atala.prism"
version = "0.1.0"

repositories {
    mavenCentral()
}

gatling {
    logLevel = "WARN"
    // set to 'ALL' for all HTTP traffic in TRACE, 'FAILURES' for failed HTTP traffic in DEBUG
    logHttp = io.gatling.gradle.LogHttp.NONE
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
