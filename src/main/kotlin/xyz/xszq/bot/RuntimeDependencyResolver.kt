package xyz.xszq.bot

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile

object RuntimeDependencyResolver {
    private const val timeoutMillis = 30_000
    private const val dependenciesEntry = "META-INF/plugin-dependencies.txt"
    private val repositories = listOf(
        Repository("阿里云镜像") { "https://maven.aliyun.com/repository/public/${it.path()}" },
        Repository("Maven中央仓库") { "https://repo1.maven.org/maven2/${it.path()}" },
        Repository("Jitpack") { "https://jitpack.io/${it.path()}" }
    )

    @JvmStatic
    fun resolveDependencies(jarFile: JarFile, libsDirectory: File): List<File> {
        return readDependencies(jarFile).map { downloadDependency(it, libsDirectory) }
    }

    @JvmStatic
    fun readDependencies(jarFile: JarFile): List<DependencyCoordinate> {
        val entry = jarFile.getJarEntry(dependenciesEntry) ?: return emptyList()
        return jarFile.getInputStream(entry).use(::parseDependencies)
    }

    @Synchronized
    @JvmStatic
    fun downloadDependency(dependency: DependencyCoordinate, libsDirectory: File): File {
        if (!libsDirectory.exists() && !libsDirectory.mkdirs() && !libsDirectory.isDirectory) {
            throw IOException("Failed to create libs directory: ${libsDirectory.absolutePath}")
        }

        val fileName = "${dependency.groupId}-${dependency.artifactId}-${dependency.version}.jar".replace("/", "-")
        val targetFile = File(libsDirectory, fileName)
        if (targetFile.exists()) {
            return targetFile
        }

        var lastException: IOException? = null
        repositories.forEach { repository ->
            try {
                println("[依赖] 尝试从 ${repository.name} 下载: $dependency")
                downloadFromUrl(repository.url(dependency), targetFile)
                println("[依赖] ${repository.name} 下载成功: ${targetFile.name}")
                return targetFile
            } catch (exception: IOException) {
                lastException = exception
                println("[依赖] 下载失败: ${exception.message}")
                Files.deleteIfExists(targetFile.toPath())
            }
        }

        println("[依赖] 所有仓库下载失败: $dependency")
        throw IOException("All repositories failed for $dependency", lastException)
    }

    private fun parseDependencies(input: InputStream): List<DependencyCoordinate> {
        BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
            return reader.lineSequence()
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.split(':')
                    if (parts.size == 3) DependencyCoordinate(parts[0], parts[1], parts[2]) else null
                }
                .toList()
        }
    }

    private fun downloadFromUrl(sourceUrl: String, targetFile: File) {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.part")
        Files.deleteIfExists(tempFile.toPath())

        val connection = openConnection(sourceUrl)
        try {
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                throw IOException("Unexpected response $statusCode from $sourceUrl")
            }

            connection.inputStream.use { input ->
                Files.newOutputStream(tempFile.toPath()).use { output ->
                    input.transferTo(output)
                }
            }

            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            connection.disconnect()
            Files.deleteIfExists(tempFile.toPath())
        }
    }

    private fun openConnection(sourceUrl: String): HttpURLConnection {
        return (URI.create(sourceUrl).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "KarenBot-Bootstrap")
        }
    }

    data class DependencyCoordinate(
        val groupId: String,
        val artifactId: String,
        val version: String
    ) {
        fun path(): String = "${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.jar"

        override fun toString(): String = "$groupId:$artifactId:$version"
    }

    private data class Repository(
        val name: String,
        val urlBuilder: (DependencyCoordinate) -> String
    ) {
        fun url(dependency: DependencyCoordinate): String = urlBuilder(dependency)
    }
}
