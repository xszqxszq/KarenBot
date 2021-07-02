plugins {
    val kotlinVersion = "1.5.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.7-M2-dev-1"
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
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("com.h2database:h2:1.4.200")
    implementation("org.jetbrains.exposed:exposed-core:0.32.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.32.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.32.1")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")
}