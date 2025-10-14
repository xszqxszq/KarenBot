val hopliteVersion: String by rootProject.extra
val korlibsVersion: String by rootProject.extra
val ktorVersion: String by rootProject.extra
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "xyz.xszq.bot"
version = "9.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":"))
    implementation("com.sksamuel.hoplite:hoplite-core:${hopliteVersion}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${hopliteVersion}")
    implementation("com.soywiz.korge:korge-core:$korlibsVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-client-okhttp:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    archiveBaseName.set("text")
    archiveClassifier.set("")

    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.Text"
        )
    }
}
kotlin {
    jvmToolchain(22)
}