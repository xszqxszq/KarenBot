package xyz.xszq

import ai.djl.pytorch.engine.PtEngine
import ai.djl.util.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.utils.info
import nu.pattern.OpenCV
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.admin.Admin
import xyz.xszq.otomadbot.admin.BadWordConfig
import xyz.xszq.otomadbot.admin.BadWordHandler
import xyz.xszq.otomadbot.admin.GroupAdmin
import xyz.xszq.otomadbot.api.ApiSettings
import xyz.xszq.otomadbot.audio.MidiShow
import xyz.xszq.otomadbot.audio.OtomadHelper
import xyz.xszq.otomadbot.audio.Speech
import xyz.xszq.otomadbot.image.*
import xyz.xszq.otomadbot.kotlin.tempDir
import xyz.xszq.otomadbot.text.*

val events = GlobalEventChannel.validate(OtomadBotCore.validator)

object OtomadBotCore : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.xszq.otomadbot.core",
        name = "OtomadBot-Core",
        version = "6.0",
    ) {
        author("xszqxszq")
    }
) {
    val validator = EventValidator()
    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }
    private val configs = mutableListOf<SafeYamlConfig<*>>(
        CooldownConfig, QuotaConfig, TextSettings,
        ApiSettings, AutoReplyConfig, YOLOv5Config, BadWordConfig, BinConfig
    )
    lateinit var modules: List<CommandModule>
    suspend fun imageReload() = withContext(Dispatchers.IO) {
        ImageMatcher.clearImages("reply")
        ImageMatcher.loadImages("reply")
        ImageMatcher.loadImages("afraid", "reply")
        ImageMatcher.loadImages("ma", "reply")
        ImageHandler.replyPic.load("reply")
        ImageHandler.replyPic.load("gif", "reply")
        ImageHandler.replyPic.load("afraid")
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
    suspend fun init() {
        OpenCV.loadLocally()
        imageReload()
        ChineseOCRLite.init()
        ai.djl.pytorch.jni.LibUtils.loadLibrary()
    }
    override fun onEnable() {
        try {
            Thread.currentThread().contextClassLoader = this::class.java.classLoader
        } finally {
            Thread.currentThread().contextClassLoader = jvmPluginClasspath.pluginClassLoader
        }
        runBlocking {
            configReload()
            logger.info { "正在初始化环境……" }
            init()
            modules = arrayListOf(ImageHandler, AutoReplyHandler, EventReaction, Admin, GroupAdmin, BadWordHandler,
                RandomHandler, MidiShow, WikiQuery, ImageProcessor, ImageTemplateHandler, Speech, SearchHandler,
                OtomadHelper
            )
            modules.forEach {
                logger.info { "正在加载 ${it.name} 模块……" }
                it.register()
            }
            logger.info { "OtomadBot 插件已加载完毕。" }
        }
    }
}