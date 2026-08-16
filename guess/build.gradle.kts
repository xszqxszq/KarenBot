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
    implementation("com.github.houbb:opencc4j:1.13.1")
    implementation("com.github.shibing624:similarity:1.1.6")
    implementation("com.soywiz.korge:korge-core:$korlibsVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
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