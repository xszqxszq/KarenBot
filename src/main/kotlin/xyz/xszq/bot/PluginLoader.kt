package xyz.xszq.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.collections.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.llm.LLMClient
import xyz.xszq.bot.message.FileManager
import xyz.xszq.bot.payload.UsersMeResponse
import xyz.xszq.bot.subscribe.SubscribeManager
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.net.URLConnection
import java.util.jar.JarFile
import xyz.xszq.bot.service.OpenAPI
import xyz.xszq.bot.service.TencentCOS
import xyz.xszq.bot.bootstrap.RuntimeDependencyResolver

/**
 * 插件加载器
 *
 * @property api 连接 QQ 服务器的客户端
 * @property database 数据库连接
 * @property llmClient LLM 客户端
 * @property subscribes 消息订阅管理器
 */
class PluginLoader(
    val api: OpenAPI,
    cos: TencentCOS,
    val database: Database,
    val llmClient: LLMClient? = null,
    val subscribes: SubscribeManager = SubscribeManager(),
    private val control: RuntimeControl,
    botInfo: UsersMeResponse ?= null
) {
    val bot = Bot(api, cos, botInfo)
    val files = FileManager()

    /**
     * 手动触发事件，CLI / 测试环境使用
     */
    suspend fun manualTrigger(event: Event) = subscribes.handle(event)

    val pluginDirectory = "plugins/"
    val libsDirectory = File("libs")

    private val logger = KotlinLogging.logger {}
    private val plugins = ConcurrentMap<String, LoadedPlugin>()

    init {
        // 禁用 Jar 缓存，保证热更新能读到新字节码
        URLConnection.setDefaultUseCaches("jar", false)
    }

    /**
     * 加载插件，文件未变化时跳过
     *
     * @param pluginFile 插件文件
     * @param force 是否强制重载
     */
    suspend fun loadOrUpdatePlugin(pluginFile: File, force: Boolean = false) {
        val pluginPath = pluginFile.absolutePath
        val lastModified = pluginFile.lastModified()

        if (!force && plugins[pluginPath]?.lastModified == lastModified)
            return

        if (plugins.containsKey(pluginPath))
            unloadPlugin(pluginPath)
        loadPlugin(pluginPath, lastModified)
    }

    /**
     * 卸载插件并关闭其类加载器
     *
     * @param pluginPath 插件路径
     */
    private suspend fun unloadPlugin(pluginPath: String) {
        val loaded = plugins.remove(pluginPath) ?: return
        loaded.plugin.unload()
        withContext(Dispatchers.IO) {
            loaded.classLoader.close()
        }
        subscribes.unsubscribe(pluginPath)
        logger.info { "[插件] 已卸载插件: $pluginPath" }
    }

    /**
     * 加载插件并实例化主类
     *
     * @param pluginPath 插件路径
     * @param lastModified 修改时间
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
                RuntimeDependencyResolver.resolveDependencies(jarFile, libsDirectory) { message ->
                    logger.info { message }
                }
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
                    (plugin as? AdminPlugin)?.control = control
                    plugin.load()
                    plugins[pluginPath] = LoadedPlugin(plugin, classLoader, lastModified)
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
     * 重载全部插件
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

    /**
     * 已加载的插件
     */
    private data class LoadedPlugin(
        val plugin: Plugin,
        val classLoader: URLClassLoader,
        val lastModified: Long
    )
}