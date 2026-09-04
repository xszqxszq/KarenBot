package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.util.JsonAsStringSerializer
import xyz.xszq.bot.util.json

/**
 * 卡片消息数据
 */
@Serializable
data class ArkData(
    val prompt: String = "",
    @SerialName("ark_type")
    val arkType: String = "",
    @SerialName("ark_name")
    val arkName: String = "",
    @Serializable(with = JsonAsStringSerializer::class)
    val fields: String = ""
) {
    var data: ArkFields? = null
        private set

    /**
     * 解析卡片消息字段
     */
    fun parsedFields() {
        data = if (fields.isBlank()) null
        else when (arkType) {
            "miniapp" -> json.decodeFromString<ArkMiniApp>(fields)
            "tuwen" -> json.decodeFromString<ArkMixed>(fields)
            else -> null
        }
    }
}