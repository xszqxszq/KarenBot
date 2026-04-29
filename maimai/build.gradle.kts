val exposedVersion: String by rootProject.extra
val hopliteVersion: String by rootProject.extra
val korlibsVersion: String by rootProject.extra
val ktorVersion: String by rootProject.extra
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "xyz.xszq.bot"
version = "9.1"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly(project(":"))
    implementation("com.github.houbb:opencc4j:1.13.1")
    implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
    implementation("com.h2database:h2:2.3.232")
    implementation("com.sksamuel.hoplite:hoplite-core:${hopliteVersion}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${hopliteVersion}")
    implementation("com.soywiz.korge:korge-core:$korlibsVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("io.github.pdvrieze.xmlutil:core-jdk:0.91.1")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.91.1")
    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-client-okhttp:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-server-core:${ktorVersion}")
    implementation("io.ktor:ktor-server-host-common:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty:${ktorVersion}")
    implementation("org.apache.lucene:lucene-core:10.2.2")
    implementation("org.apache.lucene:lucene-analysis-smartcn:10.2.2")
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-json:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-money:${exposedVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.3")
    implementation("xyz.xszq:shinobu:2.0.0")
    testImplementation(kotlin("test"))
    testImplementation(project(":"))
    testImplementation("io.mockk:mockk:1.14.3")
    testImplementation("com.soywiz.korge:korge-core:$korlibsVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("xyz.xszq:shinobu:2.0.0")
}

tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    archiveBaseName.set("maimai")
    archiveClassifier.set("")

    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.Maimai"
        )
    }
}
kotlin {
    jvmToolchain(22)
}
