package xyz.xszq.karenbot.image

import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import xyz.xszq.bot.payload.AnimeDBResult
import xyz.xszq.karenbot.api.availableUA
import xyz.xszq.karenbot.kotlin.retry
import kotlin.jvm.optionals.getOrNull

object AnimeDB {
    private const val url =
        "https://aiapiv2.animedb.cn/ai/api/detect?force_one=1&model=anime_model_lovelive&ai_detect=0"
    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val client = HttpClient(OkHttp)

    suspend fun handle(image: VfsFile): String {
        val bytes = image.readBytes()
        val type = when (FormatDetector.detect(bytes).getOrNull()) {
            Format.JPEG -> ContentType.Image.JPEG
            Format.PNG -> ContentType.Image.PNG
            Format.GIF -> ContentType.Image.GIF
            else -> throw Exception()
        }
        val suffix = when (type) {
            ContentType.Image.JPEG -> ".jpg"
            ContentType.Image.PNG -> ".png"
            ContentType.Image.GIF -> ".gif"
            else -> throw Exception()
        }
        val response = retry(3) {
            client.submitFormWithBinaryData(url, formData {
                append("\"image\"", bytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"${image.baseName}$suffix\"")
                    append(HttpHeaders.ContentType, type.toString())
                })
            }) {
                headers {
                    userAgent(availableUA)
                }
            }
        } ?: run {
            return "网络错误，请重试"
        }
        val result = runCatching {
            json.decodeFromString<AnimeDBResult>(response.bodyAsText())
        }.onFailure {
            return "网络错误，请重试"
        }.getOrThrow()

        return buildString {
            kotlin.runCatching {
                result.data.firstOrNull()?.let {
                    appendLine( "动漫人物：" + it.char.first().name )
                    appendLine( "来自动漫：" + it.char.first().from )
                    appendLine()
                    appendLine( "结果来自 AnimeTrace" )
                }
            }.onFailure {
                it.printStackTrace()
            }.getOrNull() ?: run {
                appendLine( "没有找到结果，请不要搜索动漫番剧以外的其他东西。" )
            }
        }
    }
}