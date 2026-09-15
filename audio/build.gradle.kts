val hopliteVersion: String by rootProject.extra
val korlibsVersion: String by rootProject.extra
val serializationVersion: String by rootProject.extra
plugins {
    id("bot-plugin")
}

dependencies {
    implementation("com.sksamuel.hoplite:hoplite-core:${hopliteVersion}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${hopliteVersion}")
    implementation("com.soywiz:korlibs-concurrent:$korlibsVersion")
    implementation("com.soywiz:korlibs-datastructure:$korlibsVersion")
    implementation("com.soywiz:korlibs-io:$korlibsVersion")
    implementation("com.soywiz:korlibs-io-stream:$korlibsVersion")
    implementation("com.soywiz:korlibs-io-vfs:$korlibsVersion")
    implementation("com.soywiz:korlibs-math:$korlibsVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${serializationVersion}")
    implementation("xyz.xszq:g2p-en-kt:1.0.0")
    implementation("xyz.xszq:ktpinyin:1.0.1")
    implementation("xyz.xszq:similarity-kt:1.0.0")
}

tasks.jar {
    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.audio.Audio"
        )
    }
}