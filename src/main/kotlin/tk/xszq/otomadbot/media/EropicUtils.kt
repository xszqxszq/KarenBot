@file:Suppress("unused")
package tk.xszq.otomadbot.media

import com.google.gson.Gson
import com.soywiz.korio.net.QueryString
import com.soywiz.korio.net.URL
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.getBCEToken
import tk.xszq.otomadbot.database.getCooldown
import java.io.File

const val availableUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/85.0.4183.83 Safari/537.36"

val pixivHeader = listOf(
    Pair("Referer", "https://www.pixiv.net/"),
    Pair("User-Agent", availableUA)
)

object EropicUtils: CommandUtils("eropic") {
    @CommandMatching("eropic", "get", true)
    suspend fun doHandleEropicRequest(keyword: MatchResult, event: GroupMessageEvent) = event
        .getCooldown("eropic").onReady { cooldown ->
            val result = RandomEropic.get(keyword.groupValues.getOrNull(1)?.trim())
            when {
                result.code == 0 && result.count > 0 -> {
                    val pid = result.data[0].pid.toString()
                    val author = result.data[0].author
                    val img = downloadFile(
                        result.data[0].url, pid, "." + File(URL(result.data[0].url).path).extension, tempDir, pixivHeader)
                    img.toExternalResource().use {
                        kotlin.runCatching {
                            event.quoteReply(it.uploadAsImage(event.group) + "PID: $pid\n作者：$author")
                        }.onFailure { err ->
                            event.bot.logger.error(err)
                            event.quoteReply("被腾讯拦截了o(╥﹏╥)o\n请稍后重试")
                        }
                    }
                    img.delete()
                    cooldown.update()
                }
                result.code == 429 -> event.group.sendMessage(
                    event.message.quote() + ("今天给的涩图太多了，下次想看还需${result.quota_min_ttl}秒")
                )
                result.code == 404 -> event.group.sendMessage(
                    event.message.quote() + "看起来图库里没有该关键词的涩图(R-15)哦~"
                )
                else -> event.quoteReply("无法获取涩图，api返回状态码：${result.code}")
            }
        } ?: run {
            event.quoteReply("您冲得太快了，请等候" +
                    "${event.getCooldown("eropic").remaining()/1000}秒再试")
        }
}

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
) {
    companion object {
        /**
         * Get random Eropic from api.
         * @param keyword Search keyword (Optional)
         * @return Result
         */
        suspend fun get(keyword: String? = null, proxy: String = "i.pixiv.cat"): RandomEropicResponse {
            val args = mutableListOf(
                Pair("apikey", configEropic!!.apikey),
                Pair("r18", "0"),
                Pair("num", "1"),
                Pair("proxy", proxy),
                Pair("size1200", "true")
            )
            keyword?.let { if (keyword.isNotBlank()) args.add(Pair("keyword", keyword)) }
            val url = "https://api.lolicon.app/setu/?" + QueryString.encode(*args.toTypedArray())
            val client = HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 10000
                }
            }
            val response = client.get<HttpResponse>(url)
            return Gson().fromJson(response.readText(), RandomEropicResponse::class.java)
        }
    }
}
data class RandomEropicResponse(
    val code: Int,
    val msg: String,
    val quota: Int,
    val quota_min_ttl: Int,
    val count: Int,
    val data: List<RandomEropic>
)

data class EropicDetectResult(val log_id: Long, val conclusionType: Int)

suspend fun isEropic(image: Image): Boolean {
    val token = getBCEToken(configBCE!!.antiporn_apikey, configBCE!!.antiporn_secret)
    val url = "https://aip.baidubce.com/rest/2.0/solution/v1/img_censor/v2/user_defined?access_token=$token"
    val client = HttpClient(CIO)
    val imageUrl = image.queryUrl()
    val response = client.submitForm<HttpResponse>(
        url = url, formParameters = Parameters.build {
            append("imgUrl", imageUrl)
        })
    return if (response.isSuccessful()) {
        Gson().fromJson(response.readText(), EropicDetectResult::class.java).conclusionType in arrayOf(2, 3)
    } else {
        false
    }
}