val exposedVersion: String by rootProject.extra
val hopliteVersion: String by rootProject.extra
val korlibsVersion: String by rootProject.extra
val ktorVersion: String by rootProject.extra
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
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-okhttp:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    testImplementation("io.ktor:ktor-client-mock:${ktorVersion}")
}

tasks.jar {
    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.text.Text"
        )
    }
}