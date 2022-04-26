@file:Suppress("EXPERIMENTAL_API_USAGE")

package xyz.xszq.otomadbot.api

import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.SimpleServiceMessage
import xyz.xszq.otomadbot.EventHandler
import xyz.xszq.otomadbot.QQXMLMessage
import xyz.xszq.otomadbot.availableUA


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
    val mid: String, val name: String, val approve: Boolean, val sex: String, val rank: String, val face: String,
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

typealias BilibiliApiVideoResponse = BilibiliApiResponse<BilibiliVideoInfo>
typealias BilibiliApiSearchResponse<T> = BilibiliApiResponse<BilibiliSearchResults<T>>
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
    suspend fun getCardByMid(mid: Long): BilibiliUserCard =
        client.get<BilibiliApiCardResponse>("https://account.bilibili.com/api/member/getCardByMid?mid=$mid") {
            headers {
                append("User-Agent", availableUA)
            }
        }.card
    suspend fun getMidByName(name: String): Long? =
        client.get<BilibiliApiSearchResponse<BilibiliSearchResultUser>>(
            "http://api.bilibili.com/x/web-interface/search/type") {
            parameter("search_type", "bili_user")
            parameter("keyword", name)
            headers {
                append("User-Agent", availableUA)
            }
        }.data.result.firstOrNull() ?.mid
}

@Suppress("EXPERIMENTAL_API_USAGE")
object BilibiliConverter: EventHandler("av/BV号解析", "bilibili") {
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            startsWith("av", true) { aid ->
                avToShare(aid, this)
            }
            startsWith("BV", true) { bvid ->
                bvToShare(bvid, this)
            }
        }
        super.register()
    }
    private suspend fun avToShare(aid: String, event: MessageEvent) = event.run {
        val response = BilibiliApi.queryAv(aid)
        if (response.code == 0) {
            subject.sendMessage(SimpleServiceMessage(60, QQXMLMessage {
                title(response.data.title)
                brief("[哔哩哔哩]${response.data.title}")
                summary(response.data.desc.ifBlank { response.data.dynamic })
                url("https://www.bilibili.com/video/${response.data.bvid}")
                cover(response.data.pic)
            }.build()))
        }
    }
    private suspend fun bvToShare(bvid: String, event: MessageEvent) = event.run {
        val response = BilibiliApi.queryBv(bvid)
        if (response.code == 0) {
            subject.sendMessage(SimpleServiceMessage(60, QQXMLMessage {
                title(response.data.title)
                brief("[哔哩哔哩]${response.data.title}")
                summary(response.data.desc.ifBlank { response.data.dynamic })
                url("https://www.bilibili.com/video/${response.data.bvid}")
                cover(response.data.pic)
            }.build()))
        }
    }
}