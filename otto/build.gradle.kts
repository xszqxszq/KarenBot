val hopliteVersion: String by rootProject.extra
val korlibsVersion: String by rootProject.extra
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "xyz.xszq.bot"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://raw.githubusercontent.com/DFKI-MLT/Maven-Repository/main") }
    maven { url = uri("https://nrgxnat.jfrog.io/artifactory/libs-release/") }
    maven { url = uri("https://nexus.terrestris.de/repository/public/") }
}
configurations.all {
    exclude(group = "gov.nist.math", module = "Jampack")
}
dependencies {
    compileOnly(project(":"))
    implementation("com.hankcs:hanlp:portable-1.8.6")
    implementation("com.sksamuel.hoplite:hoplite-core:${hopliteVersion}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${hopliteVersion}")
    implementation("com.soywiz.korge:korge-core:$korlibsVersion")
    implementation("de.dfki.mary:marytts-runtime:5.2.1")
    implementation("de.dfki.mary:marytts-lang-en:5.2.1")
    implementation("io.github.kasukusakura:silk-codec:0.0.5")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    archiveBaseName.set("otto")
    archiveClassifier.set("")

    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.OttoVoice"
        )
    }
}
kotlin {
    jvmToolchain(22)
}