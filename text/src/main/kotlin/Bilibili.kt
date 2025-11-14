package xyz.xszq.bot

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.get
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import xyz.xszq.bot.payload.BilibiliHtmlResponse
import xyz.xszq.bot.payload.BilibiliVideoInfo

object Bilibili {
    const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36"
    const val MARK_AVAILABLE = "<script>window.__playinfo__="
    const val MARK_BEGIN = "<script>window.__INITIAL_STATE__="
    const val MARK_END = ";(function()"
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun infoByHtml(
        bvid: String,
    ): BilibiliVideoInfo? {
        val html = client.get("https://m.bilibili.com/video/$bvid") {
            headers {
                append(HttpHeaders.UserAgent, UA)
                append(HttpHeaders.Origin, "https://m.bilibili.com")
                append("Sec-Ch-Ua",
                    "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\", \"Brave\";v=\"140\"")
                append("Sec-Ch-Ua-Mobile", "?1")
                append("Sec-Ch-Ua-Platform", "\"Android\"")
                append("Sec-Ch-Ua-Dest", "document")
                append("Sec-Ch-Ua-Mode", "navigate")
                append("Sec-Ch-Ua-Site", "none")
                append("Sec-Ch-Ua-User", "?1")
                append("Sec-Gpc", "1")
                append("Upgrade-Insecure-Requests", "1")
            }
        }.bodyAsText()
        if (MARK_AVAILABLE !in html)
            return null.also { println(html) }
        val raw = html.substringAfter(MARK_BEGIN).substringBefore(MARK_END)
        return json.decodeFromString<BilibiliHtmlResponse>(raw).data.videoData
    }
}