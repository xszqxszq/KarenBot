val skikoVersion: String by rootProject.extra
val serializationVersion: String by rootProject.extra
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
    `maven-publish`
    signing
    id("com.vanniktech.maven.publish") version "0.34.0"
}

java {
    withSourcesJar()
}
group = "xyz.xszq"
version = "2.0.1"

repositories {
    mavenCentral()
}

dependencies {
    api("io.github.pdvrieze.xmlutil:core-jdk:0.91.0-RC1")
    api("io.github.pdvrieze.xmlutil:serialization-jvm:0.91.0-RC1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    api("org.jetbrains.skiko:skiko-awt:${skikoVersion}")
    api("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:${skikoVersion}")
    api("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:${skikoVersion}")
    api("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:${skikoVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${serializationVersion}")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("xyz.xszq", "shinobu", version as String)

    pom {
        name.set("Shinobu")
        description.set("HTML-like image render library.")
        inceptionYear.set("2025")
        url.set("https://github.com/xszqxszq/KarenBot")
        licenses {
            license {
                name.set("The MIT License")
                url.set("https://opensource.org/license/mit")
            }
        }
        developers {
            developer {
                id.set("xszqxszq")
                name.set("xszqxszq")
                url.set("https://github.com/xszqxszq/")
            }
        }
        scm {
            url.set("https://github.com/xszqxszq/KarenBot/")
            connection.set("scm:git:git://github.com/xszqxszq/KarenBot.git")
            developerConnection.set("scm:git:ssh://git@github.com/xszqxszq/KarenBot.git")
        }
    }
}
signing {
    sign(publishing.publications)
}