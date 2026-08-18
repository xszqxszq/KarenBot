plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "xyz.xszq.bot"
version = project.findProperty("pluginVersion") as String

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly(project(":"))
    testImplementation(kotlin("test"))
    testImplementation(project(":"))
    testImplementation(testFixtures(project(":")))
    testImplementation("io.mockk:mockk:1.14.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
    useJUnitPlatform()
    workingDir = rootDir
}

tasks.jar {
    archiveBaseName.set(project.name)
    archiveClassifier.set("")
}

kotlin {
    jvmToolchain(22)
}
