package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Action(
    val type: Int,
    val permission: Permission,
    var data: String? = null,
    @SerialName("at_bot_show_channel_list")
    val atBotShowChannelList: Boolean ? = null,
    val reply: Boolean = false,
    val enter: Boolean = false,
    @SerialName("unsupport_tips")
    val unsupportTips: String = "兼容文本"
) {
    companion object {
        const val LINK = 0
        const val CALLBACK = 1
        const val AT = 2
    }
}