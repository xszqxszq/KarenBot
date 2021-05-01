@file:Suppress("UNUSED")
package tk.xszq.otomadbot.media

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.SimpleServiceMessage
import tk.xszq.otomadbot.*

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
object WebUtils: CommandUtils("web") {
    val client = HttpClient(CIO)
    @Command("av", "convert.av")
    suspend fun avToShare(args: Args, event: MessageEvent) = event.run {
        args.firstOrNull() ?.toLongOrNull() ?.let { aid ->
            val response = Gson().fromJson(
                client.get<HttpResponse>("http://api.bilibili.com/x/web-interface/view?aid=$aid").readText(),
                BilibiliApiVideoResponse::class.java)
            println(response)
            println(QQXMLMessage {
                brief("[哔哩哔哩]${response.data.title}")
                url("https://www.bilibili.com/video/${response.data.bvid}")
                title(response.data.title)
                summary(if (response.data.desc.isBlank()) response.data.dynamic else response.data.desc)
                cover(response.data.pic)
            }.build())
            if (response.code == 0) {
                subject.sendMessage(SimpleServiceMessage(60, QQXMLMessage {
                    title(response.data.title)
                    brief("[哔哩哔哩]${response.data.title}")
                    summary(if (response.data.desc.isBlank()) response.data.dynamic else response.data.desc)
                    url("https://www.bilibili.com/video/${response.data.bvid}")
                    cover(response.data.pic)
                }.build()))
            }
        }
    }
    @Command("BV", "convert.bv", keepRaw = true)
    suspend fun bvToShare(args: Args, event: MessageEvent) = event.run {
        args.firstOrNull() ?.let { bvid ->
            val response = Gson().fromJson(
                client.get<HttpResponse>("http://api.bilibili.com/x/web-interface/view?bvid=$bvid").readText(),
                BilibiliApiVideoResponse::class.java)
            if (response.code == 0) {
                subject.sendMessage(SimpleServiceMessage(60, QQXMLMessage {
                    title(response.data.title)
                    brief("[哔哩哔哩]${response.data.title}")
                    summary(if (response.data.desc.isBlank()) response.data.dynamic else response.data.desc)
                    url("https://www.bilibili.com/video/${response.data.bvid}")
                    cover(response.data.pic)
                }.build()))
            }
        }
    }
}