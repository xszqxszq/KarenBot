package tk.xszq.otomadbot.core.api

import com.google.gson.Gson
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.SimpleServiceMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.core.QQXMLMessage
import tk.xszq.otomadbot.core.availableUA
import tk.xszq.otomadbot.core.get
import tk.xszq.otomadbot.core.text.EventHandler

open class BilibiliApiResponse(open val code: Int, open val message: String, open val ttl: Int, open val data: Any)
data class BilibiliApiVideoResponse(val code: Int, val message: String, val ttl: Int, val data: BilibiliVideoInfo)
data class BilibiliUser(val mid: Long, val name: String, val face: String)
data class BilibiliVideoInfo(
    val bvid: String, val aid: Long, val videos: Int, val tid: Int, val tname: String,
    val copyright: Int, val pic: String, val title: String, val pubdate: Long, val ctime: Long,
    val desc: String, val state: Int, val duration: Int, val rights: HashMap<String, Int>, val owner: BilibiliUser,
    val stat: HashMap<String, Any>, val dynamic: String, val cid: Long, val dimension: HashMap<String, Short>,
    val no_cache: Boolean = false, val pages: List<Any> = emptyList(), val subtitle: HashMap<String, Any> = hashMapOf(),
    val user_garb: HashMap<String, Any> = hashMapOf()
)

@Suppress("EXPERIMENTAL_API_USAGE")
object BilibiliConverter: EventHandler("B站视频", "bilibili") {
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
        val request = Request.Builder()
            .addHeader("User-Agent", availableUA)
            .url("http://api.bilibili.com/x/web-interface/view?aid=$aid")
            .build()
        val response = Gson().fromJson(OkHttpClient().newCall(request).await().body!!.get(),
            BilibiliApiVideoResponse::class.java)
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
        val request = Request.Builder()
            .addHeader("User-Agent", availableUA)
            .url("http://api.bilibili.com/x/web-interface/view?bvid=$bvid")
            .build()
        val response = Gson().fromJson(OkHttpClient().newCall(request).await().body!!.get(),
            BilibiliApiVideoResponse::class.java)
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