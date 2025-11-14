package xyz.xszq.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.collections.*
import korlibs.io.file.VfsFile
import kotlinx.coroutines.*
import xyz.xszq.bot.message.FileManager
import xyz.xszq.bot.subscribe.SubscribeManager
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * Plugin's loader.
 * @param api `OpenAPI`.
 */
class PluginLoader(
    val api: OpenAPI,
    cos: TencentCos
) {
    val bot = Bot(api, cos, this)
    val subscribes = SubscribeManager()
    val files = FileManager()

    val pluginDirectory = "plugins/"
    val libsDirectory = "libs/"

    private val logger = KotlinLogging.logger {}
    private val loadedPlugins = ConcurrentMap<String, Plugin>()
    private val pluginTimestamps = ConcurrentMap<String, Long>()

    /**
     * Load a plugin if not exists, or reload it if changed.
     * @param pluginFile the `File` of plugin's jar.
     */
    fun loadOrUpdatePlugin(pluginFile: File, force: Boolean = false) {
        val pluginPath = pluginFile.absolutePath
        val lastModified = pluginFile.lastModified()

        if (!force && pluginTimestamps[pluginPath] == lastModified)
            return

        if (loadedPlugins.containsKey(pluginPath)) {
            unloadPlugin(pluginPath)
        }
        loadPlugin(pluginPath, lastModified)
    }
    /**
     * Load a plugin if not exists, or reload it if changed.
     * @param pluginFile the `File` of plugin's jar.
     */
    fun loadOrUpdatePlugin(pluginFile: VfsFile, force: Boolean = false) =
        loadOrUpdatePlugin(File(pluginFile.absolutePath), force)

    /**
     * Unload a plugin.
     * @param pluginPath Plugin's path.
     */
    private fun unloadPlugin(pluginPath: String) {
        val plugin = loadedPlugins[pluginPath]
        plugin?.unload()
        subscribes.unsubscribe(pluginPath)
        loadedPlugins.remove(pluginPath)
        pluginTimestamps.remove(pluginPath)
        logger.info { "[插件] 已卸载插件: $pluginPath" }
    }

    /**
     * Load a plugin.
     * @param pluginPath Plugin's path.
     * @param lastModified Last modified time of the jar file.
     */
    private fun loadPlugin(pluginPath: String, lastModified: Long) {
        val pluginFile = File(pluginPath)
        if (!pluginFile.exists()) {
            logger.error { "[插件] 文件不存在: $pluginPath" }
            return
        }

        val jarFile = JarFile(pluginFile)
        val manifest = jarFile.manifest
        val mainClassName = manifest.mainAttributes.getValue("Plugin-Class")

        if (mainClassName.isNullOrEmpty()) {
            logger.error { "[插件] 未在 Manifest 中指定主类: $pluginPath" }
            return
        }

        // 加载依赖
        val dependencies = readDependencies(jarFile)
        val dependencyFiles = dependencies.map { (groupId, artifactId, version) ->
            downloadDependency(groupId, artifactId, version)
        }

        val urls = mutableListOf<URL>().apply {
            add(pluginFile.toURI().toURL())
            dependencyFiles.forEach { file ->
                add(file.toURI().toURL())
            }
        }.toTypedArray()
        val classLoader = URLClassLoader(urls, this::class.java.classLoader)

        try {
            val pluginClass = classLoader.loadClass(mainClassName)
            val plugin = pluginClass.getDeclaredConstructor().newInstance() as? Plugin
            if (plugin != null) {
                plugin.pluginLoader = this
                plugin.plugin = pluginPath
                plugin.load()
                loadedPlugins[pluginPath] = plugin
                pluginTimestamps[pluginPath] = lastModified
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reload All plugins.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun reloadAllPlugins() {
        val pluginDir = File(pluginDirectory)
        if (!pluginDir.exists() || !pluginDir.isDirectory) {
            logger.error { "[插件] 插件目录不存在: $pluginDirectory" }
            return
        }

        runBlocking {
            pluginDir.listFiles { file -> file.extension == "jar" }?.forEach { pluginFile ->
                GlobalScope.launch(Dispatchers.IO) {
                    loadOrUpdatePlugin(pluginFile)
                }
            }
        }
    }


    /**
     * Read dependencies from plugin's META-INF/plugin-dependencies.txt
     */
    private fun readDependencies(jarFile: JarFile): List<Triple<String, String, String>> {
        val entry = jarFile.getJarEntry("META-INF/plugin-dependencies.txt") ?: return emptyList()
        return jarFile.getInputStream(entry).use { input ->
            parseDependencies(input)
        }
    }

    /**
     * Parse dependencies from input stream.
     */
    private fun parseDependencies(input: InputStream): List<Triple<String, String, String>> {
        val dependencies = mutableListOf<Triple<String, String, String>>()
        input.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val parts = trimmed.split(':')
                    if (parts.size == 3) {
                        dependencies.add(Triple(parts[0], parts[1], parts[2]))
                    } else {
                        logger.warn { "[依赖] 忽略无效依赖行: $trimmed" }
                    }
                }
            }
        }
        return dependencies
    }

    /**
     * Download dependency from maven mirror if not exists.
     */
    private fun downloadDependency(groupId: String, artifactId: String, version: String): File {
        val fileName = "$groupId-$artifactId-$version.jar".replace("/", "-")
        val libDir = File(libsDirectory).apply { mkdirs() }
        val file = File(libDir, fileName)

        if (file.exists()) {
            logger.debug { "[依赖] 使用本地依赖: ${file.name}" }
            return file
        }

        val repositories = listOf(
            { g: String, a: String, v: String ->
                "阿里云镜像" to "https://maven.aliyun.com/repository/public/${g.replace('.', '/')}/$a/$v/$a-$v.jar"
            },
            { g: String, a: String, v: String ->
                "Maven中央仓库" to "https://repo1.maven.org/maven2/${g.replace('.', '/')}/$a/$v/$a-$v.jar"
            },
            { g: String, a: String, v: String ->
                "Jitpack" to "https://jitpack.io/${g.replace('.', '/')}/$a/$v/$a-$v.jar"
            }
        )

        var lastException: Exception? = null
        for (repoBuilder in repositories) {
            try {
                val (repoName, url) = repoBuilder(groupId, artifactId, version)
                logger.info { "[依赖] 尝试从 $repoName 下载: $groupId:$artifactId:$version" }
                downloadFromUrl(url, file)
                logger.info { "[依赖] $repoName 下载成功: ${file.name}" }
                return file
            } catch (e: Exception) {
                logger.warn { "[依赖] 下载失败: ${e.message}" }
                lastException = e
            }
        }

        logger.error { "[依赖] 所有仓库下载失败: $groupId:$artifactId:$version" }
        throw RuntimeException("所有仓库下载失败: $groupId:$artifactId:$version", lastException)
    }

    private fun downloadFromUrl(url: String, targetFile: File) {
        val connection = URI(url).toURL().openConnection().apply {
            connectTimeout = 30000
            readTimeout = 60000
        }

        try {
            connection.getInputStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            if (targetFile.exists()) targetFile.delete()
            throw e
        }
    }
}