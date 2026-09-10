plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "xyz.xszq.bot"
version = project.findProperty("pluginVersion") as String

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly(project(":"))
    testImplementation(kotlin("test"))
    testImplementation(project(":"))
    testImplementation(testFixtures(project(":")))
    testImplementation("io.mockk:mockk:${project.findProperty("mockkVersion")}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${project.findProperty("coroutinesVersion")}")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("load")
    }
    workingDir = rootDir
}

tasks.register<Test>("loadTest") {
    val unitTest = tasks.named<Test>("test").get()
    group = "verification"
    description = "运行并发压测，不随 test 与 build 执行"
    testClassesDirs = unitTest.testClassesDirs
    classpath = unitTest.classpath
    useJUnitPlatform {
        includeTags("load")
    }
    workingDir = rootDir
    jvmArgs("-Djava.security.manager=allow")
    outputs.upToDateWhen { false }
    testLogging {
        showStandardStreams = true
    }
}

tasks.jar {
    archiveBaseName.set(project.name)
    archiveClassifier.set("")
}

kotlin {
    jvmToolchain(22)
}
