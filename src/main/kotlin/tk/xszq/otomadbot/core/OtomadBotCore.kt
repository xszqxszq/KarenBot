package tk.xszq.otomadbot.core

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info
import tk.xszq.otomadbot.admin.GroupAdminCommandHandler
import tk.xszq.otomadbot.api.ApiSettings
import tk.xszq.otomadbot.api.BilibiliConverter
import tk.xszq.otomadbot.api.Midishow
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
    private val registerList = arrayListOf(WelcomeHandler, Repeater, NudgeBounce, BilibiliConverter, Midishow,
        EropicHandler, ImageGeneratorHandler, ImageCommonHandler, SearchHandler, ImageEffectHandler,
        GroupAdminCommandHandler, LightAppHandler, SentimentDetector, BadWordHandler, RandomHandler
    )
    private val settings = arrayListOf(TextSettings, ApiSettings)
    override fun onEnable() {
        doCreateFolders()
        settings.forEach { it.reload() }
        logger.info { "已载入配置文件" }
        AutoReplyHandler.register()
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
        logger.info { "核心模块加载完成" }
    }
    private fun doCreateFolders() {
        arrayOf("bin", "fonts", "image", "image/reply", "image/afraid", "image/ma", "image/gif", "image/message", "music",
            "voice", "voice/message").forEach {
            Files.createDirectories(configFolder.resolve(it).toPath())
        }
    }
}