val korlibsVersion: String by rootProject.extra
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "xyz.xszq.bot"
version = "9.0"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly(project(":"))
    implementation("com.github.houbb:pinyin:0.4.0")
    implementation("com.github.houbb:opencc4j:1.7.2")
    implementation("com.github.shibing624:similarity:1.1.6")
    implementation("com.soywiz.korge:korge-core:$korlibsVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    archiveBaseName.set("guess")
    archiveClassifier.set("")

    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.Guess"
        )
    }
}
kotlin {
    jvmToolchain(22)
}