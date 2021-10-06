@file:Suppress("EXPERIMENTAL_API_USAGE")

package tk.xszq.otomadbot.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.SimpleServiceMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.EventHandler
import tk.xszq.otomadbot.QQXMLMessage
import tk.xszq.otomadbot.availableUA
import tk.xszq.otomadbot.core.OtomadBotCore
import tk.xszq.otomadbot.get

@Serializable
data class BilibiliApiVideoResponse(val code: Int, val message: String, val ttl: Int, val data: BilibiliVideoInfo)
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

object BilibiliApi: ApiClient() {
    suspend fun queryAv(aid: String): BilibiliApiVideoResponse {
        val request = Request.Builder()
            .addHeader("User-Agent", availableUA)
            .url("http://api.bilibili.com/x/web-interface/view?aid=$aid")
            .build()
        return OtomadBotCore.json.decodeFromString(OkHttpClient().newCall(request).await().body!!.get())
    }
    suspend fun queryBv(bvid: String): BilibiliApiVideoResponse {
        val request = Request.Builder()
            .addHeader("User-Agent", availableUA)
            .url("http://api.bilibili.com/x/web-interface/view?bvid=$bvid")
            .build()
        return OtomadBotCore.json.decodeFromString(OkHttpClient().newCall(request).await().body!!.get())
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
object BilibiliConverter: EventHandler("av/BV号解析", "bilibili") {
    override fun register() {
        GlobalEventChannel.subscribeMessages {
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