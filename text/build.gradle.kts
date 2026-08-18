val hopliteVersion: String by rootProject.extra
val korlibsVersion: String by rootProject.extra
val ktorVersion: String by rootProject.extra
val skikoVersion: String by rootProject.extra
val exposedVersion: String by rootProject.extra
plugins {
    id("bot-plugin")
}

dependencies {
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("com.sksamuel.hoplite:hoplite-core:${hopliteVersion}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${hopliteVersion}")
    implementation("com.soywiz.korge:korge-core:$korlibsVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-client-okhttp:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    implementation("com.microsoft.onnxruntime:onnxruntime:1.18.0")
    implementation("org.jetbrains.skiko:skiko-awt:${skikoVersion}")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:${skikoVersion}")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:${skikoVersion}")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:${skikoVersion}")
    implementation("io.ktor:ktor-server-core:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty:${ktorVersion}")
    testImplementation("io.ktor:ktor-client-mock:${ktorVersion}")
}

tasks.jar {
    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.text.Text"
        )
    }
}
