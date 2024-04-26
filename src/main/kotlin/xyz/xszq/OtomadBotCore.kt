package xyz.xszq

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.utils.info
import org.jetbrains.exposed.sql.Database
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.admin.Admin
import xyz.xszq.otomadbot.admin.BadWordConfig
import xyz.xszq.otomadbot.admin.BadWordHandler
import xyz.xszq.otomadbot.admin.GroupAdmin
import xyz.xszq.otomadbot.api.ApiSettings
import xyz.xszq.otomadbot.audio.MidiShow
import xyz.xszq.otomadbot.audio.OtomadHelper
import xyz.xszq.otomadbot.audio.Speech
import xyz.xszq.otomadbot.audio.SpeechConfig
import xyz.xszq.otomadbot.image.*
import xyz.xszq.otomadbot.mirai.SelfCheck
import xyz.xszq.otomadbot.mirai.SelfCheckConfig
import xyz.xszq.otomadbot.text.*

lateinit var mariadb: Database

//val events = GlobalEventChannel.filter {
//    it !is MessageEvent || it.bot.id !in SelfCheck.frozenList
//}.validate(OtomadBotCore.validator)
val events = GlobalEventChannel

object OtomadBotCore : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.xszq.otomadbot.core",
        name = "OtomadBot-Core",
        version = "6.0",
    ) {
        author("xszqxszq")
    }
) {
    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }
    private val configs = mutableListOf<SafeYamlConfig<*>>(
        CooldownConfig, QuotaConfig, TextSettings,
        ApiSettings, AutoReplyConfig, BadWordConfig, BinConfig, SelfCheckConfig, SubscribeTaskConfig,
        SpeechConfig, DatabaseConfig
    )
    lateinit var modules: List<CommandModule>
    var cookies = ""
    var bkn = ""
    suspend fun imageReload() = withContext(Dispatchers.IO) {
        ImageMatcher.clearImages("reply")
        ImageMatcher.loadImages("reply")
        ImageMatcher.loadImages("afraid", "reply")
        ImageMatcher.loadImages("ma", "reply")
        ImageHandler.replyPic.load("reply")
        ImageHandler.replyPic.load("gif", "reply")
        ImageHandler.replyPic.load("afraid")
        ImageProcessor.reload()
    }
    suspend fun configReload() {
        configs.forEach {
            logger.info { "正在读入 ${it.name}.yml 配置文件……" }
            it.load()
        }
    }
    suspend fun doReload() = withContext(Dispatchers.IO) {
        configReload()
        imageReload()
    }
    fun initDatabase() {
        mariadb = Database.connect(DatabaseConfig.data.url, driver = "org.mariadb.jdbc.Driver",
            DatabaseConfig.data.username, DatabaseConfig.data.password)
    }
    suspend fun init() {
        imageReload()
        initDatabase()
    }
    override fun onEnable() {
        try {
            Thread.currentThread().contextClassLoader = this::class.java.classLoader
        } finally {
            try {
                Thread.currentThread().contextClassLoader = jvmPluginClasspath.pluginClassLoader
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        runBlocking {
            configReload()
            logger.info { "正在初始化环境……" }
            init()
            modules = arrayListOf(ImageHandler, AutoReplyHandler, EventReaction, Admin, GroupAdmin, BadWordHandler,
                RandomHandler, MidiShow, WikiQuery, ImageProcessor, Speech, SearchHandler,
                OtomadHelper, SelfCheck, ArcadeQueue, EropicHandler, RandomText
            )
            modules.forEach {
                logger.info { "正在加载 ${it.name} 模块……" }
                it.register()
            }
            logger.info { "OtomadBot 插件已加载完毕。" }
        }
    }

    suspend fun doTestLoad() = withContext(Dispatchers.IO) {
        configReload()
    }
}