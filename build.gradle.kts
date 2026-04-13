val exposedVersion: String by project
val hopliteVersion: String by project
val korlibsVersion: String by project
val ktorVersion: String by project

plugins {
    application
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("com.github.johnrengelman.shadow") version "8.0.0"
}

application {
    mainClass.set("xyz.xszq.bot.Bootstrap")
}


group = "xyz.xszq.bot"
version = "9.0"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.15")
    implementation("com.qcloud:cos_api:5.6.240")
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
    implementation("com.h2database:h2:2.2.224")
    implementation("com.soywiz.korge:korge-core:$korlibsVersion")
    implementation("io.github.kasukusakura:silk-codec:0.0.5")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.1")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to "xyz.xszq.bot.Bootstrap"
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
kotlin {
    jvmToolchain(22)
}
allprojects {
    apply(plugin = "java")
    extra["exposedVersion"] = exposedVersion
    extra["hopliteVersion"] = hopliteVersion
    extra["korlibsVersion"] = korlibsVersion
    extra["ktorVersion"] = ktorVersion
    tasks.register("generatePluginDependencies") {
        val outputFile = layout.buildDirectory.file("generated/plugin-dependencies.txt")
        outputs.file(outputFile)

        doLast {
            val runtimeClasspath = configurations.runtimeClasspath.get()
            val resolvedArtifacts = runtimeClasspath.incoming.artifactView { }.artifacts.artifacts

            val dependenciesList = resolvedArtifacts.mapNotNull { artifact ->
                val componentId = artifact.id.componentIdentifier
                if (componentId is ModuleComponentIdentifier) {
                    "${componentId.group}:${componentId.module}:${componentId.version}"
                } else {
                    null
                }
            }

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
