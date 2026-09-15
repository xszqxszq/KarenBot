@file:Suppress("VulnerableLibrariesLocal")

val hopliteVersion: String by rootProject.extra
val korlibsVersion: String by rootProject.extra
val opencc4jVersion: String by rootProject.extra
val serializationVersion: String by rootProject.extra
plugins {
    id("bot-plugin")
}

repositories {
    maven { url = uri("https://raw.githubusercontent.com/DFKI-MLT/Maven-Repository/main") }
    maven { url = uri("https://nrgxnat.jfrog.io/artifactory/libs-release/") }
    maven { url = uri("https://nexus.terrestris.de/repository/public/") }
}
configurations.all {
    exclude(group = "gov.nist.math", module = "Jampack")
    exclude(group = "com.google.collections", module = "google-collections")
}
dependencies {
    implementation("com.hankcs:hanlp:portable-1.3.4")
    implementation("com.sksamuel.hoplite:hoplite-core:${hopliteVersion}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${hopliteVersion}")
    implementation("com.soywiz:korlibs-concurrent:$korlibsVersion")
    implementation("com.soywiz:korlibs-datastructure:$korlibsVersion")
    implementation("com.soywiz:korlibs-io:$korlibsVersion")
    implementation("com.soywiz:korlibs-io-stream:$korlibsVersion")
    implementation("com.soywiz:korlibs-io-vfs:$korlibsVersion")
    implementation("com.soywiz:korlibs-math:$korlibsVersion")
    implementation("de.dfki.mary:marytts-runtime:5.2.1")
    implementation("de.dfki.mary:marytts-lang-en:5.2.1")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${serializationVersion}")
    implementation("com.github.houbb:opencc4j:${opencc4jVersion}")
    implementation("xyz.xszq:similarity-kt:1.0.0")
}

tasks.jar {
    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.audio.Audio"
        )
    }
}