package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 按钮样式
 */
@Serializable
data class RenderData(
    val label: String,
    @SerialName("visited_label")
    val visitedLabel: String,
    val style: Int = GRAY
) {
    companion object {
        const val GRAY = 0
        const val BLUE = 1
        const val RED = 3
        const val FILLED_BLUE = 4
    }
}