package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 键盘按钮的点击动作
 *
 * type 决定动作类型（跳转链接、回调或 AT 等），data 承载动作对应的数据
 *
 * @property atBotShowChannelList 点击时是否展示频道选择列表
 * @property unsupportTips 场景不支持该按钮时的兜底提示文本
 */
@Serializable
data class Action(
    val type: Int,
    val permission: Permission,
    var data: String? = null,
    @SerialName("at_bot_show_channel_list")
    val atBotShowChannelList: Boolean ? = null,
    val reply: Boolean = false,
    val enter: Boolean = false,
    val anchor: Int ?= null,
    @SerialName("unsupport_tips")
    val unsupportTips: String = "兼容文本"
) {
    companion object {
        const val LINK = 0
        const val CALLBACK = 1
        const val AT = 2
    }
}