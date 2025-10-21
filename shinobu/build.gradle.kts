val korlibsVersion: String by rootProject.extra
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
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.soywiz.korge:korge-core:${korlibsVersion}")
    implementation("io.github.pdvrieze.xmlutil:core-jdk:0.91.0-RC1")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.91.0-RC1")
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
        description.set("XML based image render library.")
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