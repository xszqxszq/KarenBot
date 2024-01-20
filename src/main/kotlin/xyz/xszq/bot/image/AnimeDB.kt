package xyz.xszq.bot.image

import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import korlibs.io.file.VfsFile
import korlibs.io.file.baseName
import kotlinx.serialization.json.Json
import xyz.xszq.bot.payload.AnimeDBResult
import xyz.xszq.nereides.availableUA
import xyz.xszq.nereides.message.ark.ListArk
import xyz.xszq.nereides.retry
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

    suspend fun handle(image: VfsFile): ListArk {
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
            return ListArk.build {
                desc { "搜番结果" }
                prompt { "搜番结果" }
                text { "网络错误，请重试" }
            }
        }
        val result = runCatching {
            json.decodeFromString<AnimeDBResult>(response.bodyAsText())
        }.onFailure {
            return ListArk.build {
                desc { "搜番结果" }
                prompt { "搜番结果" }
                text { "网络错误，请重试" }
            }
        }.getOrThrow()

        return ListArk.build {
            desc { "搜番结果" }
            prompt { "搜番结果" }

            kotlin.runCatching {
                result.data.firstOrNull()?.let {
                    text { "动漫人物：" + it.char.first().name }
                    text { "来自动漫：" + it.char.first().from }
                    text { "" }
                    text { "结果来自 AnimeTrace" }
                    link("https://otmdb.cn/jump/animedb") { "AnimeTrace" }
                }
            }.onFailure {
                it.printStackTrace()
            }.getOrNull() ?: run {
                text { "没有找到结果，请不要搜索动漫番剧以外的其他东西。" }
            }
        }
    }
}