package xyz.xszq.karenbot.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.xszq.karenbot.NetworkUtils


@Serializable
open class BilibiliApiResponse<T>(val message: String, val data: T)
@Serializable
data class BilibiliUser(val mid: Long, val name: String, val face: String)
@Serializable
data class BilibiliVideoInfo(
    val bvid: String, val aid: Long, val videos: Int, val tid: Int, val tname: String,
    val copyright: Int, val pic: String, val title: String, val pubdate: Long, val ctime: Long,
    val desc: String, val state: Int, val duration: Int, val owner: BilibiliUser,
    val stat: JsonObject, val dynamic: String, val cid: Long
)
typealias BilibiliApiVideoResponse = BilibiliApiResponse<BilibiliVideoInfo>

object BilibiliApi: ApiClient() {
    suspend fun queryAv(aid: String): BilibiliApiVideoResponse =
        client.get("https://api.bilibili.com/x/web-interface/view") {
            url {
                parameters.append("aid", aid)
            }
            headers {
                append(HttpHeaders.UserAgent, availableUA)
            }
        }.body()
    suspend fun queryBv(bvid: String): BilibiliApiVideoResponse =
        client.get("https://api.bilibili.com/x/web-interface/view") {
            url {
                parameters.append("bvid", bvid)
            }
            headers {
                append("User-Agent", availableUA)
            }
        }.body()
    fun getAvBv(link: String?): String {
        link ?.let {
            return OkHttpClient.Builder()
                .addNetworkInterceptor(Interceptor { chain ->
                    chain.proceed(chain.request())
                })
                .build()
                .newCall(Request.Builder()
                    .addHeader("User-Agent", availableUA)
                    .url(it)
                    .build()
                ).execute().request.url.pathSegments.filterNot { it.isEmpty() }.last()
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
        val result = "https://b23.tv/${info.data.bvid}\n${info.data.bvid}\n" +
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