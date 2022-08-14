package xyz.xszq.otomadbot.api

import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.SimpleServiceMessage
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.xszq.otomadbot.NetworkUtils


@Serializable
open class BilibiliApiResponse<T>(val code: Int, val message: String, val ttl: Int, val data: T)
@Serializable
data class BilibiliUser(val mid: Long, val name: String, val face: String)
@Serializable
data class BilibiliVideoInfo(
    val bvid: String, val aid: Long, val videos: Int, val tid: Int, val tname: String,
    val copyright: Int, val pic: String, val title: String, val pubdate: Long, val ctime: Long,
    val desc: String, val state: Int, val duration: Int, val rights: HashMap<String, Int>, val owner: BilibiliUser,
    val stat: JsonObject, val dynamic: String, val cid: Long, val dimension: HashMap<String, Short>,
    val no_cache: Boolean = false
)
@Serializable
data class BilibiliUserCard(
    val mid: String, val name: String, val approve: Boolean, val sex: String, val rank: String, var face: String,
    val regtime: Long, val birthday: String, val sign: String, val article: Int,
    val attentions: List<Long>, val fans: Int, val friend: Int, val attention: Int, val level_info: BilibiliLevelInfo
)
@Serializable
data class BilibiliLevelInfo(
    val next_exp: Int, val current_level: Int, val current_min: Int, val current_exp: Int
)
@Serializable
data class BilibiliSearchResults<T>(
    val seid: String, val page: Int, val pagesize: Int, val numResults: Int, val numPages: Int,
    val suggest_keyword: String, val rqt_type: String, val result: List<T>
)
interface BilibiliSearchResult {
    val type: String
}
@Serializable
data class BilibiliSearchResultUser(
    override val type: String, val mid: Long, val uname: String, val usign: String, val fans: Int, val videos: Int,
    val upic: String, val room_id: Long
): BilibiliSearchResult
@Serializable
data class BilibiliOldUserCard(
    val mid: String, val name: String, val face: String, val sign: String
)
@Serializable
data class BilibiliOldUserCardRawData(
    val card: BilibiliOldUserCard, val archive_count: Int, val article_count: Int, val follower: Int,
    val like_num: Long
)

typealias BilibiliApiVideoResponse = BilibiliApiResponse<BilibiliVideoInfo>
typealias BilibiliApiSearchResponse<T> = BilibiliApiResponse<BilibiliSearchResults<T>>
typealias BilibiliApiOldUserCardResponse = BilibiliApiResponse<BilibiliOldUserCardRawData>
@Serializable
data class BilibiliApiCardResponse(val ts: Long, val code: Int, val card: BilibiliUserCard)

object BilibiliApi: ApiClient() {
    suspend fun queryAv(aid: String): BilibiliApiVideoResponse =
        client.get("http://api.bilibili.com/x/web-interface/view?aid=$aid") {
            headers {
                append("User-Agent", availableUA)
            }
        }
    suspend fun queryBv(bvid: String): BilibiliApiVideoResponse =
        client.get("http://api.bilibili.com/x/web-interface/view?bvid=$bvid") {
            headers {
                append("User-Agent", availableUA)
            }
        }
    suspend fun getCardByMid(mid: Long, keepStatic: Boolean = true): BilibiliUserCard {
        val result = client.get<BilibiliApiCardResponse>(
            "https://account.bilibili.com/api/member/getCardByMid?mid=$mid"
        ) {
            headers {
                append("User-Agent", availableUA)
            }
        }.card
        if (keepStatic && result.face.endsWith("webp") || result.face.endsWith("gif"))
            result.face = result.face + "@240w_240h_1c_1s.webp"
        return result
    }
    suspend fun getMidByName(name: String): Long? =
        client.get<BilibiliApiSearchResponse<BilibiliSearchResultUser>>(
            "http://api.bilibili.com/x/web-interface/search/type") {
            parameter("search_type", "bili_user")
            parameter("keyword", name)
            headers {
                append("User-Agent", availableUA)
            }
        }.data.result.find { it.uname == name } ?.mid
    suspend fun getUserInfo(mid: Long): BilibiliOldUserCard =
        client.get<BilibiliApiOldUserCardResponse>("http://api.bilibili.com/x/web-interface/card") {
            parameter("mid", mid)
            headers {
                append("User-Agent", availableUA)
            }
        }.data.card

    fun getAvBv(link: String?): String {
        link ?.let {
            return OkHttpClient.Builder()
                .addNetworkInterceptor(Interceptor { chain -> chain.proceed(chain.request()) })
                .build()
                .newCall(
                    Request.Builder()
                        .addHeader("User-Agent", availableUA)
                        .url(it)
                        .build()).execute().request.url.pathSegments.last()
        } ?: run {
            return ""
        }
    }
    suspend fun getVideoDetails(source: String, subject: Contact): MessageChain {
        val info = when {
            "://" in source -> return getVideoDetails(getAvBv(source), subject)
            source.startsWith("BV") -> queryBv(source)
            source.startsWith("av") -> queryAv(source.substringAfter("av"))
            else -> return buildMessageChain {  }
        }
        val result = "${info.data.bvid}\n" +
                "${info.data.title}\n" +
                "${(info.data.stat.jsonObject["view"]!!.jsonPrimitive.double).toInt()}播放 " +
                "${(info.data.stat.jsonObject["danmaku"]!!.jsonPrimitive.double).toInt()}弹幕 " +
                "${(info.data.stat.jsonObject["reply"]!!.jsonPrimitive.double).toInt()}评论\n" +
                "UP主：${info.data.owner.name}\n" +
                "简介：\n" +
                info.data.desc.take(50) + (if (info.data.desc.length > 50) "……" else "")
        val cover = NetworkUtils.downloadTempFile(info.data.pic)
        return (cover?.toExternalResource()?.use { img ->
            subject.uploadImage(img)
        } ?: "".toPlainText()) + result
    }
}