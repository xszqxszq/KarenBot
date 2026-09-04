package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.util.JsonAsStringSerializer
import xyz.xszq.bot.util.json

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

    fun parsedFields() {
        data = if (fields.isBlank()) null
        else when (arkType) {
            "miniapp" -> json.decodeFromString<ArkMiniApp>(fields)
            "tuwen" -> json.decodeFromString<ArkMixed>(fields)
            else -> null
        }
    }
}