import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version ("1.4.32")
    id("com.github.johnrengelman.shadow") version ("6.1.0")
}
configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            apiVersion = "1.4"
            languageVersion = "1.4"
        }
    }
    named<ShadowJar>("shadowJar") {
        manifest {
            attributes(mapOf("Main-Class" to "tk.xszq.otomadbot.OtomadBotKt"))
        }
    }
}

group = "tk.xszq"
version = "4.2"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("com.github.houbb:opencc4j:1.6.0")
    implementation("com.github.kilianB:JImageHash:3.0.0")
    implementation("com.gitlab.mvysny.konsume-xml:konsume-xml:0.13")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.google.zxing:javase:3.4.1")
    implementation("com.h2database:h2:1.4.199")
    implementation("com.huaban:jieba-analysis:1.0.2")
    implementation("com.soywiz.korlibs.korio:korio:2.0.10")
    implementation("com.soywiz.korlibs.korau:korau:2.0.11")
    implementation("edu.stanford.nlp:stanford-parser:3.8.0")
    implementation("edu.stanford.nlp:stanford-parser:3.8.0:models")
    implementation("mysql:mysql-connector-java:5.1.48")
    implementation("net.mamoe:mirai-core:2.7-M1-dev-2")
    implementation("net.mamoe:mirai-login-solver-selenium:1.0-dev-17")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("org.jetbrains.exposed:exposed-core:0.30.2")
    implementation("org.jetbrains.exposed:exposed-dao:0.30.2")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.30.2")
    implementation("org.jetbrains.exposed:exposed-java-time:0.30.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.32")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("org.redundent:kotlin-xml-builder:1.7.2")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    implementation("io.ktor:ktor-client-core:1.5.3")
    implementation("io.ktor:ktor-client-cio:1.5.3")
    implementation("io.ktor:ktor-server-core:1.5.3")
    implementation("io.ktor:ktor-server-netty:1.5.3")
    implementation("io.ktor:ktor-serialization:1.5.3")
}

dependencies {
    implementation(files("libs/silk4j-1.2-dev.jar"))
}