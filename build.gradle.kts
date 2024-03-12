val ktorVersion: String by project
val kotlinVersion: String by project
val korlibsVersion: String by project
val exposedVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "xyz.xszq"
version = "7.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("com.soywiz.korlibs.korau:korau-jvm:$korlibsVersion")
    implementation("com.soywiz.korlibs.korim:korim-jvm:$korlibsVersion")
    implementation("com.soywiz.korlibs.korio:korio-jvm:$korlibsVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-apache5:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("com.qcloud:cos_api:5.6.155")
    implementation("com.sksamuel.scrimage:scrimage-core:4.1.1")
    implementation("com.sksamuel.scrimage:scrimage-filters:4.1.1")
    implementation("com.sksamuel.scrimage:scrimage-formats-extra:4.1.1")
    implementation("com.sksamuel.scrimage:scrimage-webp:4.1.1")
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    implementation("io.github.biezhi:TinyPinyin:2.0.3.RELEASE")
    implementation("io.github.kasukusakura:silk-codec:0.0.5")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("net.mamoe.yamlkt:yamlkt:0.10.2")
    implementation("org.im4java:im4java:1.4.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.1.4")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.11.0")
    implementation("org.openpnp:opencv:4.3.0-3")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    implementation(kotlin("stdlib-jdk8"))
}

application {
    mainClass = "xyz.xszq.KarenBotKt"
}
kotlin {
    jvmToolchain(11)
}