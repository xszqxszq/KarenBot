@file:Suppress("SpellCheckingInspection")

plugins {
    val kotlinVersion = "1.6.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.11.0-M1"
}

group = "xyz.xszq"
version = "5.0"
repositories {
    mavenCentral()
    jcenter()
    flatDir {
        dirs("libs")
    }
}

val korlibsVersion = "2.6.2"
val ktorVersion = "1.6.8"
dependencies {
    implementation("net.mamoe.yamlkt:yamlkt:0.10.2")
    implementation("com.github.houbb:opencc4j:1.7.2")
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.google.zxing:javase:3.4.1")
    implementation("com.soywiz.korlibs.korim:korim:$korlibsVersion")
    implementation("com.soywiz.korlibs.korio:korio:$korlibsVersion")
    implementation("com.soywiz.korlibs.korau:korau:$korlibsVersion")
    implementation("dev.inmo:krontab:0.7.1")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    api("net.mamoe:mirai-silk-converter:0.0.5")
    api("io.github.kasukusakura:silk-codec:0.0.5")
}