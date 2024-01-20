package xyz.xszq.nereides.payload.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Action(
    val type: Int,
    val permission: Permission,
    @SerialName("click_limit")
    val clickLimit: Int ? = null,
    val data: String? = null,
    @SerialName("at_bot_show_channel_list")
    val atBotShowChannelList: Boolean ? = null,
    val reply: Boolean = false,
    val enter: Boolean = false,
    @SerialName("unsupport_tips")
    val unsupportTips: String = ""
) {
    companion object {
        const val LINK = 0
        const val CALLBACK = 1
        const val AT = 2
    }
}
