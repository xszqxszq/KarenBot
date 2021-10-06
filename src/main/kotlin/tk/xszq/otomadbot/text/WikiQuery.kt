@file:Suppress("EXPERIMENTAL_API_USAGE")

package tk.xszq.otomadbot.text

import com.soywiz.korio.net.URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.core.OtomadBotCore

@Serializable
data class MWSearchResultInfo(val totalhits: Int)
@Serializable
data class MWSearchResultQuery(val searchinfo: MWSearchResultInfo, val search: List<HashMap<String, String>>)
@Serializable
data class MWSearchResult(val query: MWSearchResultQuery)
object WikiQuery: EventHandler("维基搜索", "wiki", HandlerType.DEFAULT_DISABLED) {
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            startsWithSimple("维基搜索") { keyword, _ ->
                requireNot(denied) {
                    handle(keyword, this)
                }
            }
        }
        super.register()
    }
    private suspend fun handle(keyword: String, event: GroupMessageEvent) = event.run {
        if (keyword.trim() == "") return@run
        val searchUrl = "https://otomad.wiki/api.php?action=query&list=search&srwhat=title" +
                "&srnamespace=0&format=json&srsearch=" + URL.encodeComponent(keyword)
        val parseUrl = "https://otomad.wiki/api.php?action=parse&format=json&page=" + URL.encodeComponent(keyword)
        val responseExist = OkHttpClient().newCall(Request.Builder().url(parseUrl).build()).await().body!!.get()
        if ("\"code\":\"missingtitle\"" in responseExist || "\"totalhits\":0" in responseExist) {
            val responseSearch = OkHttpClient().newCall(Request.Builder().url(searchUrl).build()).await()
            if (responseSearch.isSuccessful) {
                val result = OtomadBotCore.json.decodeFromString<MWSearchResult>(responseSearch.body!!.get())
                if (result.query.searchinfo.totalhits > 10) {
                    quoteReply(
                        "https://otomad.wiki/index.php?title=Special:%E6%90%9C%E7%B4%A2" +
                                "&profile=advanced&fulltext=1&search=" + URL.encodeComponent(keyword)
                    )
                } else if (result.query.searchinfo.totalhits > 0) {
                    quoteReply(
                        "https://otomad.wiki/" +
                                URL.encodeComponent(
                                    result.query.search[0]["title"].toString()
                                        .replace(" ", "_")
                                )
                    )
                }
            }
            pass
        } else {
            quoteReply("https://otomad.wiki/" + URL.encodeComponent(keyword.replace(" ", "_")))
        }
    }
}