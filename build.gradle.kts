val korlibsVersion: String by project
val ktorVersion: String by project
val exposedVersion: String by project
plugins {
    val kotlinVersion = "1.8.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
}

group = "xyz.xszq"
version = "6.0"

repositories {
    // maven("https://maven.aliyun.com/repository/public")
    maven("https://s01.oss.sonatype.org/content/repositories/releases/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
    mavenCentral()
}
mirai {
    noTestCore = true
    setupConsoleTestRuntime {
        // 移除 mirai-core 依赖
        classpath = classpath.filter {
            !it.nameWithoutExtension.startsWith("mirai-core-jvm")
        }
    }
}
dependencies {
    compileOnly("top.mrxiaom:overflow-core-api:2.16.0-4029f15-SNAPSHOT")
    testConsoleRuntime("top.mrxiaom:overflow-core:2.16.0-4029f15-SNAPSHOT")
    implementation("com.github.houbb:opencc4j:1.7.2")
    implementation("com.github.pemistahl:lingua:1.2.2")
    implementation("com.google.zxing:core:3.5.0")
    implementation("com.google.zxing:javase:3.5.0")
    implementation("com.sksamuel.scrimage:scrimage-core:4.1.1")
    implementation("com.sksamuel.scrimage:scrimage-filters:4.1.1")
    implementation("com.sksamuel.scrimage:scrimage-formats-extra:4.1.1")
    implementation("com.sksamuel.scrimage:scrimage-webp:4.1.1")
    implementation("com.soywiz.korlibs.kmem:kmem-jvm:$korlibsVersion")
    implementation("com.soywiz.korlibs.korio:korio-jvm:$korlibsVersion")
    implementation("com.soywiz.korlibs.korau:korau-jvm:$korlibsVersion")
    implementation("com.soywiz.korlibs.korim:korim-jvm:$korlibsVersion")
    implementation("com.soywiz.korlibs.korma:korma-jvm:$korlibsVersion")
    implementation("com.twelvemonkeys.imageio:imageio-core:3.8.3")
    implementation("io.github.biezhi:TinyPinyin:2.0.3.RELEASE")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("net.mamoe.yamlkt:yamlkt:0.10.2")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.1.4")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    api("io.github.kasukusakura:silk-codec:0.0.5")
    implementation(kotlin("stdlib"))
}
