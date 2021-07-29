package tk.xszq.otomadbot.image

import com.google.gson.Gson
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.anyIsInstance
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.*
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object SearchHandler: EventHandler("搜图", "image.search") {
    override fun register() {
        GlobalEventChannel.subscribeMessages {
            startsWithSimple("搜图", true) { _, _ ->
                requireNot(denied) {
                    println("perm ok")
                    val target = if (message.anyIsInstance<Image>()) {
                        println("this")
                        this
                    } else {
                        println("no msg")
                        quoteReply("请发送想要搜索的图片（仅限二次元图片）：")
                        nextMessageEvent()
                    }
                    var tempCounter = 0
                    target.message.forEach { pic ->
                        if (pic is Image) {
                            println("search it")
                            tempCounter += 1
                            subject.sendMessage(target.message.quote() + (if (tempCounter > 1) "【图$tempCounter】" else "")
                                    + getImageSearchByUrl(pic.queryUrl()))
                        }
                    }
                }
            }
            startsWithSimple("搜番", true) { _, _ ->
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
                        }
                    }
                }
            }
        }
        super.register()
    }
    private suspend fun getImageSearchByUrl(url: String): String {
        try {
            val form = FormBody.Builder()
                .add("url", url).build()
            val request = Request.Builder()
                .url("https://saucenao.com/search.php")
                .post(form)
                .build()
            val response = OkHttpClient().newCall(request).await()
            return if (response.isSuccessful) {
                val doc = Jsoup.parse(response.body!!.get())
                val target = doc.select(".resulttablecontent")[0]
                val similarity = target.select(".resultsimilarityinfo").text()
                try {
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
                "网络连接失败，请稍后重试QWQ"
            }
        } catch (e: Exception) {
            return "网络连接失败，请稍后重试QWQ"
        }
    }
    data class TraceMoeResults(val result: List<TraceMoeResult>)
    data class TraceMoeResult(val anilist: AnilistInfo?, val filename: String, val episode: Int?, val from: Double,
                              val to: Double, val similarity: Double, val video: String, val image: String)
    data class AnilistInfo(val id: Long, val idMal: Long, val title: HashMap<String, String?>,
                           val synonyms: List<String>, val isAdult: Boolean)
    /**
     * Handle Trace.moe request.
     * @param image Image to query.
     * @param message Request message event.
     */
    private suspend fun doHandleTraceMoe(image: Image, message: MessageEvent) {
        val client = OkHttpClient().newBuilder()
            .callTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val response = client.newCall(Request.Builder().url("https://api.trace.moe/search?anilistInfo" +
                "&url=${image.queryUrl()}")
            .build()).await()
        if (response.isSuccessful) {
            val result = Gson().fromJson(response.body!!.get(), TraceMoeResults::class.java)
            try {
                if (result.result.isNotEmpty()) {
                    val realResult = result.result[0]
                    message.subject.sendMessage(
                        message.message.quote() + "[${DecimalFormat("0.##")
                            .format(realResult.similarity * 100.0)}%] " +
                        "${realResult.anilist?.title?.get("native")} " +
                                (realResult.episode ?.let {"第${realResult.episode}集 "} ?: "") +
                        String.format(
                            "%d:%02d", realResult.from.roundToInt() / 60,
                            realResult.from.roundToInt() % 60
                        ) +
                        "\n结果来自trace.moe，本bot不保证结果准确性，谢绝辱骂"
                    )
                } else {
                    message.subject.sendMessage(message.message.quote() + "没有找到相关番剧……")
                }
            } catch (e: Exception) {
                message.subject.sendMessage(message.message.quote() + "没有找到相关番剧……")
            }
        } else {
            message.subject.sendMessage(message.message.quote() + "网络错误，请重试")
            println(response.body!!.get())
        }
    }
}