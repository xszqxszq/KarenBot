val hopliteVersion: String by rootProject.extra
val korlibsVersion: String by rootProject.extra
plugins {
    id("bot-plugin")
}

dependencies {
    implementation("com.sksamuel.hoplite:hoplite-core:${hopliteVersion}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${hopliteVersion}")
    implementation("com.soywiz.korge:korge-core:$korlibsVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
}

tasks.jar {
    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.admin.Admin"
        )
    }
}
