@file:Suppress("EXPERIMENTAL_API_USAGE")

package xyz.xszq.otomadbot.api

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import xyz.xszq.otomadbot.availableUA

@Serializable
data class RandomEropic(
    val pid: Long,
    val p: Int,
    val uid: Long,
    val title: String,
    val author: String,
    val ext: String = "",
    val urls: Map<String, String>,
    val r18: Boolean,
    val width: Int,
    val height: Int,
    val tags: List<String>
) {
    companion object {
        val pixivHeader = listOf(
            Pair("Referer", "https://www.pixiv.net/"),
            Pair("User-Agent", availableUA)
        )
        val client = HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 10000
                requestTimeoutMillis = 30000
            }
        }
        suspend fun get(keyword: String? = null, proxy: String = "0", r18: Boolean = false, num: Int = 1,
                        size: List<String> = listOf("regular")): RandomEropicResponse {
            return client.post("https://api.lolicon.app/setu/v2") {
                contentType(ContentType.Application.Json)
                body = RandomEropicRequest(
                    r18 = if (r18) 2 else 0,
                    num = num,
                    keyword = keyword ?.let { keyword.ifBlank { "" } } ?: "",
                    tag = if (num != 1 && (keyword == null || keyword.isBlank()))
                        listOf(listOf("全裸")) else emptyList(),
                    size = size,
                    proxy = proxy
                )
            }
        }
    }
}
@Serializable
data class RandomEropicRequest(
    val r18: Int,
    val num: Int,
    val keyword: String = "",
    val tag: List<List<String>> = listOf(),
    val size: List<String> = listOf("regular"),
    val proxy: String = "",
    val uid: List<Int> = emptyList()
)

@Serializable
data class RandomEropicResponse(
    val data: List<RandomEropic>,
    val error: String
)