package xyz.xszq.bot.text

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.xszq.nereides.NetworkUtils
import xyz.xszq.nereides.availableUA
import xyz.xszq.nereides.message.MessageChain
import xyz.xszq.nereides.message.plus
import xyz.xszq.nereides.message.toImage
import xyz.xszq.nereides.message.toPlainText

object Bilibili {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 600000L
            requestTimeoutMillis = 600000L
            socketTimeoutMillis = 600000L
        }
        expectSuccess = false
    }
    suspend fun queryAv(aid: String): BilibiliApiResponse<BilibiliVideoInfo> =
        client.get("https://api.bilibili.com/x/web-interface/view") {
            url {
                parameters.append("aid", aid)
            }
            headers {
                append(HttpHeaders.UserAgent, availableUA)
            }
        }.body()
    suspend fun queryBv(bvid: String): BilibiliApiResponse<BilibiliVideoInfo> =
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
                .newCall(
                    Request.Builder()
                    .addHeader("User-Agent", availableUA)
                    .url(it)
                    .build()
                ).execute().request.url.pathSegments.filterNot { it.isEmpty() }.last()
        } ?: run {
            return ""
        }
    }
    suspend fun getVideoDetails(source: String): MessageChain {
        val info = when {
            "://" in source -> return getVideoDetails(getAvBv(source))
            source.startsWith("BV") -> queryBv(source)
            source.startsWith("av") -> queryAv(source.substringAfter("av"))
            else -> return MessageChain()
        }
        val result = buildString {
            appendLine()
            appendLine("https://b23.tv/${info.data.bvid}\n${info.data.bvid}")
            appendLine(info.data.title)
            append("${(info.data.stat.jsonObject["view"]!!.jsonPrimitive.double).toInt()}播放 ")
            append("${(info.data.stat.jsonObject["danmaku"]!!.jsonPrimitive.double).toInt()}弹幕 ")
            append("${(info.data.stat.jsonObject["reply"]!!.jsonPrimitive.double).toInt()}评论")
            appendLine()
            appendLine("UP主：${info.data.owner.name}")
            appendLine("简介：")
            appendLine(info.data.desc.take(50) + (if (info.data.desc.length > 50) "……" else ""))
        }.trimEnd()
        val cover = NetworkUtils.downloadTempFile(info.data.pic)
        return (cover?.toImage() ?: "".toPlainText()) + result.toPlainText()
    }
}