val hopliteVersion: String by rootProject.extra
val korlibsVersion: String by rootProject.extra
plugins {
    kotlin("jvm")
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
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    testImplementation(kotlin("test"))
    testImplementation(project(":"))
    testImplementation("io.mockk:mockk:1.14.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
    useJUnitPlatform()
    workingDir = rootDir
}
tasks.jar {
    archiveBaseName.set("admin")
    archiveClassifier.set("")

    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.Admin"
        )
    }
}
kotlin {
    jvmToolchain(22)
}