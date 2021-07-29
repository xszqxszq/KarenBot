plugins {
    val kotlinVersion = "1.5.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.7-M2"
}

group = "tk.xszq"
version = "5.0"
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("com.charleskorn.kaml:kaml:0.34.0")
    implementation("com.github.houbb:opencc4j:1.6.1")
    implementation("com.github.kilianB:JImageHash:3.0.0")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.google.zxing:javase:3.4.1")
    implementation("com.h2database:h2:1.4.197")
    implementation("com.soywiz.korlibs.korim:korim:2.2.0")
    implementation("com.soywiz.korlibs.korio:korio:2.2.0")
    implementation("com.soywiz.korlibs.korau:korau:2.2.0")
    implementation("org.jetbrains.exposed:exposed-core:0.32.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.32.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.32.1")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")
    implementation(files("libs/silk4j-1.2-dev.jar"))
}