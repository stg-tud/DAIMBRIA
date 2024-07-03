plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
}

group = "de.daimpl"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("org.jgrapht:jgrapht-core:1.5.1")
    implementation("org.springframework:spring-expression:6.1.10")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("com.github.cmdjulian:jdsl:1.0.4")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
}

tasks.test {
    useJUnitPlatform()
}