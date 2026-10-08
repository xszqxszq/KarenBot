val korlibsVersion: String by rootProject.extra
val hopliteVersion: String by rootProject.extra
val exposedVersion: String by rootProject.extra
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
    implementation("org.eclipse.angus:angus-mail:2.0.3")
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${exposedVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${serializationVersion}")
}

tasks.jar {
    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.feedback.Feedback"
        )
    }
}