package xyz.xszq.nereides.payload.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    }
}
