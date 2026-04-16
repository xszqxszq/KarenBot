package xyz.xszq.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.collections.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import xyz.xszq.bot.message.FileManager
import xyz.xszq.bot.subscribe.SubscribeManager
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.net.URLConnection
import java.util.jar.JarFile

/**
 * Plugin's loader.
 * @param api `OpenAPI`.
 */
class PluginLoader(
    val api: OpenAPI,
    cos: TencentCos,
    val database: Database
) {
    val bot = Bot(api, cos, this)
    val subscribes = SubscribeManager()
    val files = FileManager()

    val pluginDirectory = "plugins/"
    val libsDirectory = File("libs")

    private val logger = KotlinLogging.logger {}
    private val loadedPlugins = ConcurrentMap<String, Plugin>()
    private val pluginTimestamps = ConcurrentMap<String, Long>()
    private val pluginClassLoaders = ConcurrentMap<String, URLClassLoader>()

    init {
        URLConnection.setDefaultUseCaches("jar", false)
    }

    /**
     * Load a plugin if not exists, or reload it if changed.
     * @param pluginFile the `File` of plugin's jar.
     */
    suspend fun loadOrUpdatePlugin(pluginFile: File, force: Boolean = false) {
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
     * Unload a plugin.
     * @param pluginPath Plugin's path.
     */
    private suspend fun unloadPlugin(pluginPath: String) {
        val plugin = loadedPlugins[pluginPath]
        plugin?.unload()
        pluginClassLoaders.remove(pluginPath)?.let { classLoader ->
            withContext(Dispatchers.IO) {
                classLoader.close()
            }
        }
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
    private suspend fun loadPlugin(pluginPath: String, lastModified: Long) {
        val pluginFile = File(pluginPath)
        if (!pluginFile.exists()) {
            logger.error { "[插件] 文件不存在: $pluginPath" }
            return
        }

        withContext(Dispatchers.IO) {
            JarFile(pluginFile)
        }.use { jarFile ->
            val manifest = jarFile.manifest
            val mainClassName = manifest.mainAttributes.getValue("Plugin-Class")

            if (mainClassName.isNullOrEmpty()) {
                logger.error { "[插件] 未在 Manifest 中指定主类: $pluginPath" }
                return
            }

            val dependencyFiles = withContext(Dispatchers.IO) {
                RuntimeDependencyResolver.resolveDependencies(jarFile, libsDirectory)
            }

            val urls = mutableListOf<URL>().apply {
                add(pluginFile.toURI().toURL())
                dependencyFiles.forEach { file ->
                    add(file.toURI().toURL())
                }
            }.toTypedArray()
            val classLoader = URLClassLoader(urls, this::class.java.classLoader)

            val error = runCatching {
                val pluginClass = classLoader.loadClass(mainClassName)
                val plugin = pluginClass.getDeclaredConstructor().newInstance() as? Plugin
                if (plugin != null) {
                    plugin.pluginLoader = this
                    plugin.plugin = pluginPath
                    plugin.load()
                    loadedPlugins[pluginPath] = plugin
                    pluginTimestamps[pluginPath] = lastModified
                    pluginClassLoaders[pluginPath] = classLoader
                }
            }.exceptionOrNull()

            if (error != null) {
                withContext(Dispatchers.IO) {
                    classLoader.close()
                }
                error.printStackTrace()
            }
        }
    }

    /**
     * Reload All plugins.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun reloadAllPlugins() {
        val pluginDir = File(pluginDirectory)
        if (!pluginDir.exists() || !pluginDir.isDirectory) {
            logger.error { "[插件] 插件目录不存在: $pluginDirectory" }
            return
        }

        val files = pluginDir.listFiles { file -> file.extension == "jar" } ?: emptyArray()

        coroutineScope {
            files.map { pluginFile ->
                launch {
                    loadOrUpdatePlugin(pluginFile)
                }
            }
        }
    }
}
