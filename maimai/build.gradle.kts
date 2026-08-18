val exposedVersion: String by rootProject.extra
val hopliteVersion: String by rootProject.extra
val korlibsVersion: String by rootProject.extra
val ktorVersion: String by rootProject.extra
val luceneVersion: String by rootProject.extra
val xmlutilVersion: String by rootProject.extra
val serializationVersion: String by rootProject.extra
val h2Version: String by rootProject.extra
val mariadbVersion: String by rootProject.extra
val ksoupVersion: String by rootProject.extra
val opencc4jVersion: String by rootProject.extra
plugins {
    id("bot-plugin")
}

repositories {
    mavenLocal()
}

dependencies {
    implementation("com.github.houbb:opencc4j:${opencc4jVersion}")
    implementation("com.fleeksoft.ksoup:ksoup:${ksoupVersion}")
    implementation("com.h2database:h2:${h2Version}")
    implementation("com.sksamuel.hoplite:hoplite-core:${hopliteVersion}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${hopliteVersion}")
    implementation("com.soywiz.korge:korge-core:$korlibsVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("io.github.pdvrieze.xmlutil:core-jdk:${xmlutilVersion}")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:${xmlutilVersion}")
    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-client-okhttp:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-server-core:${ktorVersion}")
    implementation("io.ktor:ktor-server-host-common:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty:${ktorVersion}")
    implementation("org.apache.lucene:lucene-core:${luceneVersion}")
    implementation("org.apache.lucene:lucene-analysis-smartcn:${luceneVersion}")
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-json:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-money:${exposedVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${serializationVersion}")
    implementation("org.mariadb.jdbc:mariadb-java-client:${mariadbVersion}")
    implementation(project(":shinobu"))
    testImplementation("com.soywiz.korge:korge-core:$korlibsVersion")
    testImplementation(project(":shinobu"))
}

tasks.jar {
    manifest {
        attributes(
            "Plugin-Class" to "xyz.xszq.bot.maimai.Maimai"
        )
    }
}
