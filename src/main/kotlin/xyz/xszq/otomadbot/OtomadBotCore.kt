package xyz.xszq.otomadbot

import com.soywiz.korio.async.launchImmediately
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.utils.info
import net.mamoe.yamlkt.Yaml
import xyz.xszq.otomadbot.admin.BotAdminCommandHandler
import xyz.xszq.otomadbot.admin.GroupAdminCommandHandler
import xyz.xszq.otomadbot.api.ApiSettings
import xyz.xszq.otomadbot.api.BilibiliConverter
import xyz.xszq.otomadbot.api.Midishow
import xyz.xszq.otomadbot.api.PicaComic
import xyz.xszq.otomadbot.audio.*
import xyz.xszq.otomadbot.core.BinConfig
import xyz.xszq.otomadbot.core.CooldownConfig
import xyz.xszq.otomadbot.core.QuotaConfig
import xyz.xszq.otomadbot.image.*
import xyz.xszq.otomadbot.text.*
import java.nio.file.Files

object OtomadBotCore : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.xszq.otomadbot.core",
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
        RandomHandler, WikiQuery, TTSHandler, BPMAnalyser, AudioEffectHandler,
        RandomMusic, ForwardMessageConstructor, RequestAccept, AudioCommonHandler, ImageTemplate
    //, ScheduledTaskHandler
    ) // TODO: 这么多是怎么会是呢，是不是该搞点自动的
    private val settings = listOf(TextSettings, ApiSettings, BinConfig, CooldownConfig, QuotaConfig,
        ImageTemplateConfigs)
    private val dataFiles = listOf(ScheduledMessageData)
    val json = Json { isLenient = true; ignoreUnknownKeys = true }
    val yaml = Yaml {}
    override fun onEnable() {
        doCreateFolders()
        doLoadLibraries()
        logger.info { "库文件加载完毕" }
        doTest()
        logger.info { "运行前测试完毕" }
        laterFindBot()
        launchImmediately {
            doReload()
            registerList.forEach {
                kotlin.runCatching {
                    it.register()
                }.onFailure {
                    logger.error(it)
                }
            }
            logger.info { "加载完成" }
        }
    }
    private suspend fun doLoadReplyPic() {
        kotlin.runCatching {
            ImageMatcher.clearImages("reply")
            ImageMatcher.loadImages("reply")
            ImageMatcher.loadImages("afraid", "reply")
            ImageMatcher.loadImages("ma", "reply")
            ImageMatcher.loadImages("mtf")
            ImageCommonHandler.replyPic.load("reply")
            ImageCommonHandler.replyPic.load("gif", "reply")
            ImageCommonHandler.replyPic.load("afraid")
            logger.info { "图片自动回复功能载入完成" }
        }.onFailure {
            logger.error(it)
        }
    }
    suspend fun doReload() {
        settings.forEach { it.reload() }
        logger.info { "配置文件载入完毕" }
        dataFiles.forEach { it.reload() }
        logger.info { "数据文件载入完毕" }
        doLoadReplyPic()
        AutoReplyHandler.reloadConfig()
        PicaComic.reloadConfig()
        BadWordHandler.reloadConfig()
    }
    private fun doTest() {
    }
    private fun doLoadLibraries() {
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