@file:Suppress("EXPERIMENTAL_API_USAGE")

package tk.xszq.otomadbot.image

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.anyIsInstance
import org.jsoup.Jsoup
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.ApiSettings
import tk.xszq.otomadbot.core.*
import java.text.DecimalFormat
import kotlin.math.roundToInt

object SearchHandler: EventHandler("搜图", "image.search") {
    private val cooldown = Cooldown("search")
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            startsWithSimple("搜图", true) { _, _ ->
                ifReady(cooldown) {
                    requireNot(denied) {
                        val target = if (message.anyIsInstance<Image>()) {
                            this
                        } else {
                            quoteReply("请发送想要搜索的图片（仅限二次元图片）：")
                            nextMessageEvent()
                        }
                        var tempCounter = 0
                        target.message.forEach { pic ->
                            if (pic is Image) {
                                tempCounter += 1
                                subject.sendMessage(
                                    target.message.quote() + (if (tempCounter > 1) "【图$tempCounter】" else "")
                                            + getImageSearchByUrl(pic.queryUrl())
                                )
                                update(cooldown)
                            }
                        }
                    }
                } ?: run {
                    quoteReply("搜得太快了~\n还需要等 ${remaining(cooldown)} 秒")
                }
            }
            startsWithSimple("搜番", true) { _, _ ->
                ifReady(cooldown) {
                    requireNot(denied) {
                        val target = if (message.anyIsInstance<Image>()) {
                            this
                        } else {
                            quoteReply("请发送想要搜索的番剧截图：")
                            nextMessageEvent()
                        }
                        target.message.forEach { pic ->
                            if (pic is Image) {
                                doHandleTraceMoe(pic, target)
                                update(cooldown)
                            }
                        }
                    }
                } ?: run {
                    quoteReply("搜得太快了~\n还需要等 ${remaining(cooldown)} 秒")
                }
            }
        }
        super.register()
    }
    val client = HttpClient {
        install(HttpTimeout) {
            socketTimeoutMillis = 10000
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 10000
        }
        engine {
            proxy =
                if (ApiSettings.proxy.type.lowercase() == "socks")
                    ProxyBuilder.socks(ApiSettings.proxy.addr, ApiSettings.proxy.port)
                else
                    ProxyBuilder.http("http://" + ApiSettings.proxy.addr + ":" + ApiSettings.proxy.port)
        }
    }
    private suspend fun getImageSearchByUrl(url: String): String {
        kotlin.runCatching {
            val response = client.submitForm<HttpResponse>(url = "https://saucenao.com/search.php",
                formParameters = Parameters.build {
                    append("url", url)
                })
            return if (response.status == HttpStatusCode.OK) {
                val doc = Jsoup.parse(response.readText())
                val target = doc.select(".resulttablecontent")[0]
                val similarity = target.select(".resultsimilarityinfo").text()
                if (similarity.substringBefore('%').toDouble() < 70) {
                    "没有找到相关图片……"
                } else try {
                    val name = target.select(".resulttitle").text()
                    val links = target.select(".resultcontentcolumn>a")
                    var link = target.select(".resultmiscinfo>a").attr("href")
                    if (link == "") target.select(".resulttitle>a").attr("href")
                    if (link == "") link = links[0].attr("href")
                    val author =
                        when {
                            target.select(".resulttitle").toString().contains(
                                "<strong>Creator: </strong>",
                                ignoreCase = true
                            ) -> target.select(
                                ".resulttitle"
                            ).text()
                            links.size == 1 -> links[0].text()
                            links.size == 0 -> "Various Artist"
                            else -> links[1].text()
                        }
                    "[$similarity] $name by $author\n$link"
                } catch (e: Exception) {
                    "[$similarity] " + target.select(".resultcontent").text()
                } + "\n结果来自SauceNao，本bot不保证结果准确性，谢绝辱骂"
            } else {
                println(response.status)
                "网络连接失败，请稍后重试QWQ"
            }
        }.onFailure {
            return "网络连接失败，请稍后重试QWQ"
        }
        return ""
    }
    @Serializable
    data class TraceMoeResults(val result: List<TraceMoeResult>)
    @Serializable
    data class TraceMoeResult(val anilist: AnilistInfo?=null, val filename: String, val episode: String?=null,
                              val from: Double, val to: Double, val similarity: Double, val video: String,
                              val image: String)
    @Serializable
    data class AnilistInfo(val id: Long, val idMal: Long, val title: HashMap<String, String?>,
                           val synonyms: List<String>, val isAdult: Boolean)
    /**
     * Handle Trace.moe request.
     * @param image Image to query.
     * @param message Request message event.
     */
    private suspend fun doHandleTraceMoe(image: Image, message: MessageEvent) {
        val response = client.get<HttpResponse>("https://api.trace.moe/search?anilistInfo" +
                "&url=${image.queryUrl()}")
        if (response.status == HttpStatusCode.OK) {
            val result = OtomadBotCore.json.decodeFromString<TraceMoeResults>(response.readText())
            try {
                if (result.result.isNotEmpty()) {
                    val realResult = result.result[0]
                    if (realResult.similarity < 0.7) {
                        message.subject.sendMessage(message.message.quote() + "没有找到相关番剧……")
                        return
                    }
                    var returnText = "[${DecimalFormat("0.##")
                        .format(realResult.similarity * 100.0)}%] " +
                            "${realResult.anilist?.title?.get("native")} "
                    try {
                        returnText += (realResult.episode ?.let {"第${realResult.episode}集 "} ?: "")
                    } catch (e: Exception) {
                        pass
                    }
                    returnText +=
                        String.format(
                            "%d:%02d", realResult.from.roundToInt() / 60,
                            realResult.from.roundToInt() % 60
                        )
                    returnText += "\n结果来自trace.moe，本bot不保证结果准确性，谢绝辱骂"
                    message.subject.sendMessage(
                        message.message.quote() + returnText
                    )
                } else {
                    message.subject.sendMessage(message.message.quote() + "没有找到相关番剧……")
                }
            } catch (e: Exception) {
                message.subject.sendMessage(message.message.quote() + "没有找到相关番剧……")
            }
        } else {
            message.subject.sendMessage(message.message.quote() + "网络错误，可能当前搜番请求过多，请重试")
            println(response.readText())
        }
    }
}