plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    application
}

ktlint {
    disabledRules.set(listOf("no-wildcard-imports"))
}

repositories {
    mavenCentral()
    maven("https://schlaubi.jfrog.io/artifactory/envconf/")
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        name = "ktor-eap"
    }
}

dependencies {
    implementation("dev.schlaubi", "envconf", "1.1")

    implementation("io.ktor", "ktor-client-okhttp", "1.5.2")
    implementation("io.ktor", "ktor-client-serialization", "1.5.2")

    implementation("com.google.code.gson:gson:2.8.9")

    implementation("io.github.redouane59.twitter", "twittered", "2.13")

    implementation("ch.qos.logback", "logback-classic", "1.2.6")
    implementation("io.github.microutils", "kotlin-logging", "2.0.11")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        }
    }
}

application {
    mainClass.set("de.stckoverflw.reversetweets.LauncherKt")
}
