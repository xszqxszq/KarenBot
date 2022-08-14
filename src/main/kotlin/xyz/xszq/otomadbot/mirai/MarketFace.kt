package xyz.xszq.otomadbot.mirai

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import net.mamoe.mirai.message.data.MarketFace

@Serializable
data class MarketFaceXYData(
    val appData: Map<String, String> = mapOf(), val timestamp: Long = 0L, val data: MarketFaceData
)
@Serializable
data class MarketFaceData(
    val baseInfo: List<MarketFaceBaseInfo> = listOf(), val md5Info: List<MarketFaceMd5Info>
)
@Serializable
data class MarketFaceBaseInfo(
    val name: String = "", val _id: String = "", val id: String = ""
)
@Serializable
data class MarketFaceMd5Info(val name: String, val md5: String)
@Suppress("EXPERIMENTAL_API_USAGE")
suspend fun MarketFace.getAttr() = HttpClient {
    install(JsonFeature) {
        serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}
    .get<MarketFaceXYData>("https://gxh.vip.qq.com/qqshow/admindata/comdata/vipEmoji_item_$id/xydata.json")
    .data
suspend fun MarketFace.queryUrl(size: Int = 300) = getAttr().md5Info.run {
    val md5 = find { "[${it.name}]" == name } ?.md5 ?: first().md5
    "https://gxh.vip.qq.com/club/item/parcel/item/${md5.subSequence(0, 2)}/$md5/${size}x${size}.png"
}