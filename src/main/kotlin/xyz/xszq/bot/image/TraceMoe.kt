package xyz.xszq.bot.image

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import xyz.xszq.bot.payload.TraceMoeResults
import java.text.DecimalFormat
import kotlin.math.roundToInt

object TraceMoe {
    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
        engine {
            config {
                followRedirects(true)
            }
        }
    }
    suspend fun doHandleTraceMoe(url: String): String {
        val response = kotlin.runCatching {
            client.get("https://api.trace.moe/search?anilistInfo&url=$url")
        }.onFailure {
            return "网络错误，可能当前搜番请求过多，请重试"
        }.getOrThrow()
        if (response.status == HttpStatusCode.OK) {
            val result = json.decodeFromString<TraceMoeResults>(response.bodyAsText())
            try {
                if (result.result.isEmpty()) {
                    return "没有找到相关番剧……"
                }
                val realResult = result.result[0]
                if (realResult.similarity < 0.7) {
                    return "没有找到相关番剧……"
                }
                return buildString {
                    try {
                        val similarity = DecimalFormat("0.##").format(realResult.similarity * 100.0)
                        append("[$similarity%] ")
                        append("${realResult.anilist?.title?.get("native")} ")
                        append(realResult.episode ?.let {"第${realResult.episode}集 "} ?: "")
                        append("%d:%02d".format(
                            realResult.from.roundToInt() / 60,
                            realResult.from.roundToInt() % 60
                        ))
                        append("\n" +
                                "结果来自 TraceMoe")
                    } catch (_: Exception) {
                        return "没有找到相关番剧……"
                    }
                }
            } catch (e: Exception) {
                return "没有找到相关番剧……"
            }
        } else {
            println(response.bodyAsText())
            return "网络错误，可能当前搜番请求过多，请重试"
        }
    }
}