val korlibsVersion: String by project
val ktorVersion: String by project
plugins {
    val kotlinVersion = "1.6.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.12.0"
}

group = "xyz.xszq"
version = "6.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    maven("https://s01.oss.sonatype.org/content/repositories/releases/")
    mavenCentral()
}

dependencies {
    implementation("ai.djl:api:0.18.0")
    implementation("ai.djl.opencv:opencv:0.18.0")
    implementation("ai.djl.pytorch:pytorch-native-cpu:1.11.0")
    implementation("ai.djl.pytorch:pytorch-model-zoo:0.18.0")
    implementation("ai.djl.pytorch:pytorch-engine:0.18.0")
    implementation("com.github.houbb:opencc4j:1.7.2")
    implementation("com.github.pemistahl:lingua:1.2.2")
    implementation("com.google.zxing:core:3.5.0")
    implementation("com.google.zxing:javase:3.5.0")
    implementation("com.sksamuel.scrimage:scrimage-core:4.0.31")
    implementation("com.soywiz.korlibs.korio:korio-jvm:$korlibsVersion")
    implementation("com.soywiz.korlibs.korau:korau-jvm:$korlibsVersion")
    implementation("com.soywiz.korlibs.korim:korim-jvm:$korlibsVersion")
    implementation("com.soywiz.korlibs.korma:korma-jvm:$korlibsVersion")
    implementation("com.twelvemonkeys.imageio:imageio-core:3.8.2")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.8.2")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    implementation("net.mamoe.yamlkt:yamlkt:0.10.2")
    implementation("org.jsoup:jsoup:1.15.2")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    implementation("xyz.xszq:mirai-multi-account:1.0.4")
    api("net.mamoe:mirai-silk-converter:0.0.5")
    api("io.github.kasukusakura:silk-codec:0.0.5")
}
