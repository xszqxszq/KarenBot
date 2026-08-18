val korlibsVersion: String by rootProject.extra
plugins {
    id("bot-plugin")
}

dependencies {
    implementation("com.github.houbb:pinyin:0.4.0")
    implementation("com.github.houbb:opencc4j:1.13.1")
    implementation("com.github.shibing624:similarity:1.1.6")
    implementation("com.soywiz.korge:korge-core:$korlibsVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

tasks.jar {
    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.guess.Guess"
        )
    }
}
