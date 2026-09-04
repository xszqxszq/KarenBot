package xyz.xszq.bot.bootstrap

import java.io.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.jar.JarFile

/**
 * 运行时依赖解析
 */
object RuntimeDependencyResolver {
    private const val TIMEOUT_MILLIS = 30_000L
    private const val DEPENDENCIES_ENTRY = "META-INF/plugin-dependencies.txt"
    private val repositories = listOf(
        Repository("阿里云镜像") { "https://maven.aliyun.com/repository/public/${it.path()}" },
        Repository("Maven中央仓库") { "https://repo1.maven.org/maven2/${it.path()}" },
        Repository("Jitpack") { "https://jitpack.io/${it.path()}" }
    )
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /**
     * 解析插件全部依赖并下载
     *
     * @param jarFile 插件的 JarFile
     * @param libsDirectory 依赖目录
     * @param log 下载进度输出
     * @return 依赖文件
     */
    fun resolveDependencies(
        jarFile: JarFile,
        libsDirectory: File,
        log: (String) -> Unit = ::println
    ): List<File> = readDependencies(jarFile).map { dependency ->
        downloadDependency(dependency, libsDirectory, log)
    }

    /**
     * 读取插件依赖
     *
     * @param jarFile 插件的 JarFile
     * @return 依赖列表
     */
    fun readDependencies(jarFile: JarFile): List<DependencyCoordinate> {
        val entry = jarFile.getJarEntry(DEPENDENCIES_ENTRY) ?: return emptyList()
        return jarFile.getInputStream(entry).use(::parseDependencies)
    }

    /**
     * 下载单个依赖
     *
     * @param dependency 依赖
     * @param libsDirectory 依赖目录
     * @param log 下载进度输出
     * @return 依赖文件
     */
    @Synchronized
    fun downloadDependency(
        dependency: DependencyCoordinate,
        libsDirectory: File,
        log: (String) -> Unit = ::println
    ): File {
        require(libsDirectory.exists() || libsDirectory.mkdirs() || libsDirectory.isDirectory) {
            "Failed to create libs directory: ${libsDirectory.absolutePath}"
        }
        val fileName = "${dependency.groupId}-${dependency.artifactId}-${dependency.version}.jar"
            .replace("/", "-")
        val targetFile = File(libsDirectory, fileName)
        if (targetFile.exists())
            return targetFile

        repositories.forEach { repository ->
            log("尝试从 ${repository.name} 下载: $dependency")
            val downloaded = runCatching {
                download(repository.url(dependency), targetFile)
            }.onFailure { e ->
                log("下载失败: ${e.message}")
                Files.deleteIfExists(targetFile.toPath())
            }.isSuccess
            if (downloaded) {
                log("${repository.name} 下载成功: ${targetFile.name}")
                return targetFile
            }
        }
        throw IOException("All repositories failed for $dependency")
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

    /**
     * 下载 URL 到本地文件
     *
     * @param url 下载地址
     * @param targetFile 目标文件
     */
    private fun download(url: String, targetFile: File) {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.part")
        Files.deleteIfExists(tempFile.toPath())
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofMillis(TIMEOUT_MILLIS))
            .header("User-Agent", "KarenBot-Bootstrap")
            .GET()
            .build()
        runCatching {
            val response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofFile(tempFile.toPath())
            )
            if (response.statusCode() !in 200..299)
                throw IOException("Unexpected response ${response.statusCode()} from $url")
            Files.move(
                tempFile.toPath(), targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
            )
        }.onFailure {
            Files.deleteIfExists(tempFile.toPath())
        }.getOrThrow()
    }

    /**
     * Maven 依赖
     *
     * @param groupId 依赖的组 ID
     * @param artifactId 依赖的构件 ID
     * @param version 依赖版本
     */
    data class DependencyCoordinate(
        val groupId: String,
        val artifactId: String,
        val version: String
    ) {
        fun path(): String {
            val groupPath = groupId.replace('.', '/')
            return "$groupPath/$artifactId/$version/$artifactId-$version.jar"
        }

        override fun toString(): String = "$groupId:$artifactId:$version"
    }

    private data class Repository(
        val name: String,
        val urlBuilder: (DependencyCoordinate) -> String
    ) {
        fun url(dependency: DependencyCoordinate): String = urlBuilder(dependency)
    }
}