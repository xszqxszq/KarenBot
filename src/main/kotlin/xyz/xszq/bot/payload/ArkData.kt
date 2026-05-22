package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import xyz.xszq.bot.json

@Serializable
data class ArkData(
    val prompt: String = "",
    @SerialName("ark_type")
    val arkType: String = "",
    @SerialName("ark_name")
    val arkName: String = "",
    val fields: JsonElement? = null
) {
    fun parsedFields(): ArkFields? {
        val raw = fields ?: return null
        return when (arkType) {
            "miniapp" -> json.decodeFromJsonElement<ArkMiniApp>(raw)
            "tuwen" -> json.decodeFromJsonElement<ArkMixed>(raw)
            else -> null
        }
    }
}
