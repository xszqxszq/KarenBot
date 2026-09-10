@file:Suppress("VulnerableLibrariesLocal")

val exposedVersion: String by project
val hopliteVersion: String by project
val korlibsVersion: String by project
val ktorVersion: String by project
val luceneVersion: String by project
val xmlutilVersion: String by project
val serializationVersion: String by project
val h2Version: String by project
val mariadbVersion: String by project
val ksoupVersion: String by project
val opencc4jVersion: String by project
val mockkVersion: String by project
val coroutinesVersion: String by project
val logbackVersion: String by project
val bcprovVersion: String by project

plugins {
    application
    `java-test-fixtures`
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "8.0.0"
}

application {
    mainClass.set("xyz.xszq.bot.bootstrap.Bootstrap")
}


group = "xyz.xszq.bot"
version = "9.1"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    api("io.mockk:mockk:$mockkVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.h2database:h2:$h2Version")
    implementation("com.mysql:mysql-connector-j:9.3.0")
    implementation("com.qcloud:cos_api:5.6.277") {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    }
    implementation("com.github.houbb:opencc4j:${opencc4jVersion}")
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
    implementation("com.soywiz.korge:korge-core:$korlibsVersion")
    implementation("io.github.kasukusakura:silk-codec:0.0.5")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("org.bouncycastle:bcprov-jdk18on:$bcprovVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.mariadb.jdbc:mariadb-java-client:$mariadbVersion")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testFixturesImplementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    testFixturesImplementation("com.soywiz.korge:korge-core:$korlibsVersion")
    testFixturesImplementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    testFixturesImplementation("org.bouncycastle:bcprov-jdk18on:$bcprovVersion")
    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    testFixturesApi("io.ktor:ktor-client-mock:$ktorVersion")
    testFixturesApi("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testFixturesApi("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    testFixturesApi("io.ktor:ktor-server-core:$ktorVersion")
    testFixturesApi("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    testFixturesApi("io.ktor:ktor-server-netty:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to "xyz.xszq.bot.bootstrap.Bootstrap"
        )
    }
    from({
        configurations.runtimeClasspath.get()
            .filter { file ->
                file.name.startsWith("kotlin-stdlib") || file.name.startsWith("annotations-")
            }
            .map(::zipTree)
    })
}
tasks.register("allPlugins") {
    group = "shadow"
    description = "Build all Plugins."

    val subProjects = subprojects
        .filter { it.name !in listOf("KarenBot", "shinobu") }

    subProjects.forEach { project ->
        dependsOn(project.tasks.named("jar"))
    }
    doLast {
        val pluginsDir = project.rootDir.resolve("plugins")
        pluginsDir.mkdirs()

        subProjects.forEach { project ->
            val jarFile = project.layout.buildDirectory.dir("libs").get().asFile
                .listFiles()
                ?.find { it.name.endsWith(".jar") }

            jarFile?.copyTo(pluginsDir.resolve(jarFile.name), overwrite = true)
        }
    }
}
tasks.named<Delete>("clean") {
    delete(layout.projectDirectory.dir("buildSrc/build"))
}
kotlin {
    jvmToolchain(22)
}
allprojects {
    apply(plugin = "java")
    extra["exposedVersion"] = exposedVersion
    extra["hopliteVersion"] = hopliteVersion
    extra["korlibsVersion"] = korlibsVersion
    extra["ktorVersion"] = ktorVersion
    extra["luceneVersion"] = luceneVersion
    extra["xmlutilVersion"] = xmlutilVersion
    extra["serializationVersion"] = serializationVersion
    extra["h2Version"] = h2Version
    extra["mariadbVersion"] = mariadbVersion
    extra["ksoupVersion"] = ksoupVersion
    extra["opencc4jVersion"] = opencc4jVersion
    extra["mockkVersion"] = mockkVersion
    extra["coroutinesVersion"] = coroutinesVersion
    tasks.register("generatePluginDependencies") {
        val outputFile = layout.buildDirectory.file("generated/plugin-dependencies.txt")
        inputs.file(layout.projectDirectory.file("build.gradle.kts"))
        inputs.files(configurations.runtimeClasspath)
        outputs.file(outputFile)

        doLast {
            val runtimeClasspath = configurations.runtimeClasspath.get()
            val resolvedArtifacts = runtimeClasspath.incoming.artifactView { }.artifacts.artifacts

            val dependenciesList = resolvedArtifacts.mapNotNull { artifact ->
                when (val componentId = artifact.id.componentIdentifier) {
                    is ModuleComponentIdentifier -> {
                        "${componentId.group}:${componentId.module}:${componentId.version}"
                    }
                    is ProjectComponentIdentifier -> {
                        rootProject.allprojects.find { it.path == componentId.projectPath }
                            ?.let { "${it.group}:${it.name}:${it.version}" }
                    }
                    else -> null
                }
            }.distinct().sorted()

            val output = outputFile.get().asFile
            output.parentFile.mkdirs()
            output.writeText(dependenciesList.joinToString("\n"))
        }
    }
    tasks.named<Jar>("jar") {
        dependsOn("generatePluginDependencies")
        from(layout.buildDirectory.dir("generated")) {
            include("plugin-dependencies.txt")
            into("META-INF")
        }
    }
}
