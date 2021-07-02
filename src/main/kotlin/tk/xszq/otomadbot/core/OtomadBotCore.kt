package tk.xszq.otomadbot.core

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info
import tk.xszq.otomadbot.core.api.BilibiliConverter
import tk.xszq.otomadbot.core.api.Midishow
import tk.xszq.otomadbot.core.text.*

object OtomadBotCore : KotlinPlugin(
    JvmPluginDescription(
        id = "tk.xszq.otomadbot.core.OtomadBotCore",
        name = "OtomadBot-Core",
        version = "5.0",
    ) {
        author("xszqxszq")
    }
) {
    private val registerList = arrayListOf(WelcomeHandler, Repeater, NudgeBounce, BilibiliConverter, Midishow)
    override fun onEnable() {
        TextSettings.reload()
        ApiSettings.reload()
        logger.info { "已载入配置文件" }
        AutoReplyHandler.register()
        logger.info { "自动回复功能初始化完成" }
        registerList.forEach {
            it.register()
        }
        logger.info { "核心模块加载完成" }
    }
}