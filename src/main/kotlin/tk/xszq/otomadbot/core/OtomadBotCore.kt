package tk.xszq.otomadbot.core

import io.github.mzdluo123.silk4j.LameCoder
import io.github.mzdluo123.silk4j.NativeLibLoader
import io.github.mzdluo123.silk4j.SilkCoder
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.utils.info
import tk.xszq.otomadbot.admin.BotAdminCommandHandler
import tk.xszq.otomadbot.admin.GroupAdminCommandHandler
import tk.xszq.otomadbot.api.*
import tk.xszq.otomadbot.audio.*
import tk.xszq.otomadbot.image.*
import tk.xszq.otomadbot.text.*
import java.nio.file.Files

object OtomadBotCore : KotlinPlugin(
    JvmPluginDescription(
        id = "tk.xszq.otomadbot.core.OtomadBotCore",
        name = "OtomadBot-Core",
        version = "5.0",
    ) {
        author("xszqxszq")
    }
) {
    lateinit var bot: Bot
    val registerList = arrayListOf(AutoReplyHandler, WelcomeHandler, Repeater, NudgeBounce, BilibiliConverter, Midishow,
        EropicHandler, ImageGeneratorHandler, ImageCommonHandler, SearchHandler, ImageEffectHandler,
        GroupAdminCommandHandler, BotAdminCommandHandler, LightAppHandler, SentimentDetector, BadWordHandler,
        RandomHandler, WikiQuery, TTSHandler, BPMAnalyser, ScheduledTaskHandler, MaimaiDXHandler, AudioEffectHandler
    ) // TODO: 这么多是怎么会是呢，是不是该搞点自动的
    private val settings = listOf(TextSettings, ApiSettings, BinConfig, CooldownConfig, MaimaiConfig)
    private val dataFiles = listOf(ScheduledMessageData)
    val json = Json { isLenient = true; ignoreUnknownKeys = true }
    override fun onEnable() {
        doCreateFolders()
        doLoadLibraries()
        logger.info { "库文件加载完毕" }
        doTest()
        logger.info { "运行前测试完毕" }
        laterFindBot()
        doReload()
        doInitH2Images("reply")
        doInitH2Images("afraid", "reply")
        doInitH2Images("ma", "reply")
        ImageCommonHandler.replyPic.load("reply")
        ImageCommonHandler.replyPic.load("gif", "reply")
        ImageCommonHandler.replyPic.load("afraid")
        logger.info { "自动回复功能初始化完成" }
        registerList.forEach {
            it.register()
        }
        logger.info { "加载完成" }
    }
    fun doReload() {
        settings.forEach { it.reload() }
        logger.info { "配置文件载入完毕" }
        dataFiles.forEach { it.reload() }
        logger.info { "数据文件载入完毕" }
    }
    private fun doTest() {
        SilkCoder()
        LameCoder()
    }
    private fun doLoadLibraries() {
        NativeLibLoader.load() // 我超，为什么没有自动加载
    }
    private fun doCreateFolders() {
        arrayOf("bin", "fonts", "image", "image/reply", "image/afraid", "image/ma", "image/gif", "image/message", "music",
            "voice", "voice/message").forEach {
            Files.createDirectories(configFolder.resolve(it).toPath())
        }
    }
    private fun laterFindBot() {
        GlobalEventChannel.subscribeAlways<BotOnlineEvent> {
            OtomadBotCore.bot = this.bot
        }
    }
}