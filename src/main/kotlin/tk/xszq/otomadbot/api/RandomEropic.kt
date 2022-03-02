@file:Suppress("EXPERIMENTAL_API_USAGE")

package tk.xszq.otomadbot.api

import com.soywiz.korio.net.QueryString
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.availableUA
import tk.xszq.otomadbot.core.OtomadBotCore
import tk.xszq.otomadbot.get
import java.util.concurrent.TimeUnit

@Serializable
data class RandomEropic(
    val pid: Long,
    val p: Int,
    val uid: Long,
    val title: String,
    val author: String,
    val url: String,
    val r18: Boolean,
    val width: Int,
    val height: Int,
    val tags: List<String>
): ApiClient() {
    companion object {
        val pixivHeader = listOf(
            Pair("Referer", "https://www.pixiv.net/"),
            Pair("User-Agent", availableUA)
        )
        suspend fun get(keyword: String? = null, proxy: String = "", r18: Boolean = false): RandomEropicResponse {
            ApiSettings.list["eropic"] ?.let { config ->
                val args = mutableListOf(
                    Pair("apikey", config.apikey),
                    Pair("r18", if (r18) "1" else "0"),
                    Pair("num", "1"),
                    Pair("proxy", proxy),
                    Pair("size1200", "true")
                )
                keyword?.let { if (keyword.isNotBlank()) args.add(Pair("keyword", keyword)) }
                val url = "${config.url}?" + QueryString.encode(*args.toTypedArray())
                val client = OkHttpClient().newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .build()
                val response = client.newCall(request).await()
                return OtomadBotCore.json.decodeFromString(response.body!!.get())
            } ?: run {
                println("警告：ApiSettings 中 eropic 未配置！")
            }
            return RandomEropicResponse(-114514, "Config error", 114514, 0, 0,
                emptyList())
        }
    }
}
@Serializable
data class RandomEropicResponse(
    val code: Int,
    val msg: String,
    val quota: Int? = null,
    val quota_min_ttl: Int? = null,
    val count: Int,
    val data: List<RandomEropic>
)